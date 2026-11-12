package cn.edu.sdua.geren.service;

import cn.edu.sdua.geren.dto.Dtos;
import cn.edu.sdua.geren.entity.DocumentChunk;
import cn.edu.sdua.geren.entity.KbDocument;
import cn.edu.sdua.geren.repository.ChunkRepository;
import cn.edu.sdua.geren.repository.DocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 文档管理模块：上传 -> 文本提取 -> 清洗 -> 分割 -> 向量化 -> 状态管理 -> 删除，
 * 所有操作均以 userId 隔离，保证权限控制。
 */
@Service
public class DocumentService {

    private static final Set<String> ALLOWED = Set.of("txt", "md", "markdown", "log", "csv", "pdf", "docx");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_CHUNK_LEN = 500;
    private static final int CHUNK_OVERLAP = 60;

    private final DocumentRepository docRepo;
    private final ChunkRepository chunkRepo;
    private final EmbeddingService embedding;

    @Value("${app.upload-dir:./data/uploads}")
    private String uploadDir;

    public DocumentService(DocumentRepository docRepo, ChunkRepository chunkRepo, EmbeddingService embedding) {
        this.docRepo = docRepo;
        this.chunkRepo = chunkRepo;
        this.embedding = embedding;
    }

    public Dtos.DocumentVO upload(MultipartFile file, Long userId) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }
        String name = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
        int dot = name.lastIndexOf('.');
        String type = dot < 0 ? "txt" : name.substring(dot + 1).toLowerCase();
        if (!ALLOWED.contains(type)) {
            throw new IllegalArgumentException("暂不支持 ." + type + " 格式，支持：txt, md, log, csv, pdf, docx");
        }

        KbDocument doc = new KbDocument();
        doc.setUserId(userId);
        doc.setName(name);
        doc.setFileType(type);
        doc.setFileSize(file.getSize());
        doc.setStatus("PARSING");
        doc.setChunkCount(0);
        doc = docRepo.save(doc);

        // 原始文件落盘：./data/uploads/{userId}/doc_{id}_{安全文件名}
        Path dir = Paths.get(uploadDir, String.valueOf(userId));
        Files.createDirectories(dir);
        String safeName = name.replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5._-]", "_");
        Path target = dir.resolve("doc_" + doc.getId() + "_" + safeName);
        Files.write(target, file.getBytes());
        doc.setFilePath(target.toString());

        try {
            // 解析与预处理
            String raw = extractText(type, file.getBytes());
            String clean = clean(raw);
            if (clean.isBlank()) {
                throw new IllegalArgumentException("未能从文档中提取到有效文本");
            }
            doc.setStatus("VECTORIZING");
            docRepo.save(doc);

            // 分割 + 向量化 + 入库
            List<String> pieces = split(clean);
            List<DocumentChunk> chunks = new ArrayList<>();
            for (int i = 0; i < pieces.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(doc.getId());
                chunk.setUserId(userId);
                chunk.setChunkIndex(i);
                chunk.setContent(pieces.get(i));
                chunk.setEmbedding(embedding.encode(embedding.embedOne(pieces.get(i))));
                chunks.add(chunk);
            }
            chunkRepo.saveAll(chunks);
            doc.setChunkCount(chunks.size());
            doc.setStatus("READY");
            doc.setErrorMsg(null);
        } catch (Exception e) {
            doc.setStatus("FAILED");
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            doc.setErrorMsg(msg.length() > 480 ? msg.substring(0, 480) : msg);
        }
        return toVO(docRepo.save(doc));
    }

    public List<Dtos.DocumentVO> list(Long userId) {
        return docRepo.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toVO).toList();
    }

    public Dtos.DocumentVO detail(Long id, Long userId) {
        return toVO(getOwned(id, userId));
    }

    @Transactional
    public void delete(Long id, Long userId) throws Exception {
        KbDocument doc = getOwned(id, userId);
        chunkRepo.deleteByDocumentId(doc.getId());
        if (doc.getFilePath() != null) {
            Files.deleteIfExists(Paths.get(doc.getFilePath()));
        }
        docRepo.delete(doc);
    }

    private KbDocument getOwned(Long id, Long userId) {
        KbDocument doc = docRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (!doc.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该文档");
        }
        return doc;
    }

    /** 文本提取：按格式分派解析器 */
    private String extractText(String type, byte[] bytes) throws Exception {
        switch (type) {
            case "pdf":
                try (PDDocument pdf = Loader.loadPDF(bytes)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true);
                    return stripper.getText(pdf);
                }
            case "docx":
                try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                    return new XWPFWordExtractor(docx).getText();
                }
            default:
                return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /** 文本清洗：统一换行、去除控制字符/零宽字符、压缩连续空白与多余空行 */
    private String clean(String text) {
        if (text == null) return "";
        String s = text.replace("\r\n", "\n").replace('\r', '\n');
        s = s.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u200B-\\u200D\\uFEFF]", "");
        s = s.replaceAll("[ \\t\\u3000]{2,}", " ");
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s.trim();
    }

    /** 文本分割：按句子边界累积成约500字的片段，相邻片段保留60字重叠以维持上下文 */
    private List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[。！？!?；;\\n])|(?<=\\.\\s)");
        StringBuilder buf = new StringBuilder();
        for (String sentence : sentences) {
            if (sentence == null || sentence.isBlank()) continue;
            if (buf.length() > 0 && buf.length() + sentence.length() > MAX_CHUNK_LEN) {
                chunks.add(buf.toString().trim());
                String tail = buf.substring(Math.max(0, buf.length() - CHUNK_OVERLAP));
                buf.setLength(0);
                buf.append(tail);
            }
            buf.append(sentence);
        }
        if (!buf.toString().isBlank()) {
            chunks.add(buf.toString().trim());
        }
        return chunks;
    }

    private Dtos.DocumentVO toVO(KbDocument doc) {
        String statusText = switch (doc.getStatus()) {
            case "PARSING" -> "解析中";
            case "VECTORIZING" -> "向量化中";
            case "READY" -> "就绪";
            case "FAILED" -> "失败";
            default -> doc.getStatus();
        };
        return new Dtos.DocumentVO(doc.getId(), doc.getName(), doc.getFileType(), doc.getFileSize(),
                doc.getStatus(), statusText, doc.getChunkCount(), doc.getErrorMsg(),
                doc.getCreatedAt() == null ? null : doc.getCreatedAt().format(FMT));
    }
}
