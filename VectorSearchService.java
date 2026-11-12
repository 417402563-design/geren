package cn.edu.sdua.geren.service;

import cn.edu.sdua.geren.entity.DocumentChunk;
import cn.edu.sdua.geren.repository.ChunkRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 向量检索：存储结构为 H2 表 kb_chunk（原文 content + 向量 embedding，按 userId 天然隔离），
 * 查询时对当前用户的全部向量做余弦相似度 TopK 排序。
 */
@Service
public class VectorSearchService {

    public record Scored(DocumentChunk chunk, double score) {}

    private final ChunkRepository chunkRepo;
    private final EmbeddingService embedding;

    public VectorSearchService(ChunkRepository chunkRepo, EmbeddingService embedding) {
        this.chunkRepo = chunkRepo;
        this.embedding = embedding;
    }

    public List<Scored> search(Long userId, String query, int topK) {
        double[] q = embedding.embedOne(query);
        return chunkRepo.findByUserId(userId).stream()
                .map(c -> new Scored(c, embedding.cosine(q, embedding.decode(c.getEmbedding()))))
                .filter(s -> s.score() > 0.05)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(topK)
                .toList();
    }
}
