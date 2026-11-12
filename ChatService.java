package cn.edu.sdua.geren.service;

import cn.edu.sdua.geren.dto.Dtos;
import cn.edu.sdua.geren.entity.Conversation;
import cn.edu.sdua.geren.entity.DocumentChunk;
import cn.edu.sdua.geren.entity.Message;
import cn.edu.sdua.geren.repository.ConversationRepository;
import cn.edu.sdua.geren.repository.DocumentRepository;
import cn.edu.sdua.geren.repository.MessageRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 智能问答模块 + 对话历史管理模块：
 * 问答流程 = 检索(Retrieval) -> 上下文增强(Augmentation) -> 生成 -> 引用溯源；
 * 历史能力 = 保存、查看、继续对话（携带会话内最近消息做多轮增强）。
 */
@Service
public class ChatService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int TOP_K = 5;
    private static final String SYS_PROMPT =
            "你是一个严谨的个人知识库助手。请仅依据给定参考资料回答问题，并在引用处标注[编号]；"
            + "若资料不足以回答，请明确说明未在知识库中找到相关内容。";

    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;
    private final DocumentRepository docRepo;
    private final VectorSearchService searchService;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Value("${app.llm.api-key:}")
    private String apiKey;
    @Value("${app.llm.base-url:https://api.openai.com/v1}")
    private String baseUrl;
    @Value("${app.llm.model:gpt-4o-mini}")
    private String model;

    public ChatService(ConversationRepository convRepo, MessageRepository msgRepo,
                       DocumentRepository docRepo, VectorSearchService searchService) {
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
        this.docRepo = docRepo;
        this.searchService = searchService;
    }

    @Transactional
    public Dtos.AskResp ask(Long userId, Dtos.AskReq req) {
        if (req.question() == null || req.question().isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        String question = req.question().trim();

        // 继续对话：定位已有会话；否则新建会话（标题取首个问题）
        Conversation conversation;
        List<Message> history = List.of();
        if (req.conversationId() != null) {
            conversation = getOwnedConversation(req.conversationId(), userId);
            history = lastMessages(conversation.getId(), 6);
        } else {
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setTitle(titleOf(question));
            conversation = convRepo.save(conversation);
        }

        // 1. 检索：仅在当前用户的向量数据内
        List<VectorSearchService.Scored> hits = searchService.search(userId, question, TOP_K);

        // 2. 智能增强：拼装带编号的参考资料上下文，同时生成引用溯源
        Map<Long, String> docNames = docNames(hits);
        StringBuilder context = new StringBuilder();
        List<Dtos.Citation> citations = new ArrayList<>();
        for (int i = 0; i < hits.size(); i++) {
            DocumentChunk chunk = hits.get(i).chunk();
            String docName = docNames.getOrDefault(chunk.getDocumentId(), "已删除文档");
            context.append('[').append(i + 1).append("] 《").append(docName).append("》：")
                    .append(trim(chunk.getContent(), 800)).append('\n');
            citations.add(new Dtos.Citation(chunk.getDocumentId(), docName, chunk.getChunkIndex(),
                    trim(chunk.getContent(), 160), round(hits.get(i).score())));
        }

        // 3. 生成：配置了 LLM 则生成式回答，否则抽取式兜底；调用失败自动降级
        String answer;
        if (apiKey != null && !apiKey.isBlank()) {
            String generated = null;
            try {
                generated = callLlm(context.toString(), question, history);
            } catch (Exception ignored) {
            }
            answer = generated != null && !generated.isBlank() ? generated : localAnswer(question, citations);
        } else {
            answer = localAnswer(question, citations);
        }

        // 4. 保存历史记录（用户问题 + 回答 + 引用）
        msgRepo.save(buildMessage(conversation.getId(), "user", question, null));
        msgRepo.save(buildMessage(conversation.getId(), "assistant", answer, toJson(citations)));
        conversation.setUpdatedAt(LocalDateTime.now());
        convRepo.save(conversation);

        return new Dtos.AskResp(conversation.getId(), answer, citations);
    }

    public List<Dtos.ConversationVO> listConversations(Long userId) {
        return convRepo.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(c -> new Dtos.ConversationVO(c.getId(), c.getTitle(),
                        c.getCreatedAt().format(FMT), c.getUpdatedAt().format(FMT)))
                .toList();
    }

    public List<Dtos.MessageVO> listMessages(Long conversationId, Long userId) {
        getOwnedConversation(conversationId, userId);
        return msgRepo.findByConversationIdOrderByIdAsc(conversationId).stream()
                .map(m -> new Dtos.MessageVO(m.getId(), m.getRole(), m.getContent(),
                        parseCitations(m.getCitations()), m.getCreatedAt().format(FMT)))
                .toList();
    }

    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conversation = getOwnedConversation(conversationId, userId);
        msgRepo.deleteByConversationId(conversation.getId());
        convRepo.delete(conversation);
    }

    private Conversation getOwnedConversation(Long id, Long userId) {
        Conversation conversation = convRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在"));
        if (!conversation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该对话");
        }
        return conversation;
    }

    private List<Message> lastMessages(Long conversationId, int n) {
        List<Message> all = msgRepo.findByConversationIdOrderByIdAsc(conversationId);
        return all.subList(Math.max(0, all.size() - n), all.size());
    }

    private Message buildMessage(Long conversationId, String role, String content, String citations) {
        Message m = new Message();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        m.setCitations(citations);
        return m;
    }

    private Map<Long, String> docNames(List<VectorSearchService.Scored> hits) {
        Set<Long> ids = new HashSet<>();
        for (VectorSearchService.Scored s : hits) {
            ids.add(s.chunk().getDocumentId());
        }
        Map<Long, String> map = new HashMap<>();
        if (!ids.isEmpty()) {
            docRepo.findAllById(ids).forEach(d -> map.put(d.getId(), d.getName()));
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private String callLlm(String context, String question, List<Message> history) {
        StringBuilder user = new StringBuilder();
        user.append("参考资料：\n").append(context.isBlank() ? "（无检索结果）\n" : context);
        if (!history.isEmpty()) {
            user.append("\n历史对话：\n");
            for (Message m : history) {
                user.append("user".equals(m.getRole()) ? "问：" : "答：")
                        .append(trim(m.getContent(), 200)).append('\n');
            }
        }
        user.append("\n问题：").append(question);

        List<Map<String, String>> msgs = List.of(
                Map.of("role", "system", "content", SYS_PROMPT),
                Map.of("role", "user", "content", user.toString()));
        Map<String, Object> resp = RestClient.create().post()
                .uri(baseUrl.trim() + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", model, "messages", msgs))
                .retrieve().body(Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    /** 本地抽取式回答：汇总最相关片段并标注引用编号 */
    private String localAnswer(String question, List<Dtos.Citation> citations) {
        if (citations.isEmpty()) {
            return "抱歉，未在您的知识库中检索到与「" + question + "」相关的内容。\n"
                    + "请先在【知识库管理】中上传相关文档，再进行提问。";
        }
        StringBuilder sb = new StringBuilder("根据知识库检索结果，为您找到以下相关内容：\n\n");
        for (int i = 0; i < citations.size(); i++) {
            Dtos.Citation c = citations.get(i);
            sb.append('[').append(i + 1).append("]《").append(c.documentName())
                    .append("》第").append(c.chunkIndex() + 1).append("段（相似度 ")
                    .append(Math.round(c.score() * 100)).append("%）：\n")
                    .append(c.snippet()).append("\n\n");
        }
        sb.append("以上内容由向量检索自动定位。配置大模型 API Key 后，可获得基于上述资料的生成式综合回答。");
        return sb.toString();
    }

    private String titleOf(String question) {
        return question.length() > 30 ? question.substring(0, 30) + "…" : question;
    }

    private String trim(String text, int max) {
        String s = text.replaceAll("\\s+", " ").trim();
        return s.length() > max ? s.substring(0, max) + "……" : s;
    }

    private double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Dtos.Citation> parseCitations(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<List<Dtos.Citation>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
