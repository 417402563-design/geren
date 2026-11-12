package cn.edu.sdua.geren.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 向量生成：优先调用 OpenAI 兼容 /embeddings 接口；
 * 未配置 api-key 或调用失败时，退化为内置本地哈希向量
 * （中文单字+二元组、英文数字整词，512维，L2归一化），保证系统开箱可用。
 */
@Service
public class EmbeddingService {

    public static final int LOCAL_DIM = 512;

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Value("${app.llm.api-key:}")
    private String apiKey;
    @Value("${app.llm.base-url:https://api.openai.com/v1}")
    private String baseUrl;
    @Value("${app.llm.embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    public boolean useRemote() {
        return apiKey != null && !apiKey.isBlank();
    }

    public double[] embedOne(String text) {
        if (useRemote()) {
            try {
                return remoteEmbed(text);
            } catch (Exception ignored) {
                // 远端失败则退回本地向量
            }
        }
        return localEmbed(text);
    }

    /** 向量存储：以 JSON 数组字符串形式写入 H2 的 kb_chunk.embedding 列 */
    public String encode(double[] vector) {
        try {
            return mapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new RuntimeException("向量序列化失败", e);
        }
    }

    public double[] decode(String json) {
        try {
            return mapper.readValue(json, double[].class);
        } catch (Exception e) {
            return new double[0];
        }
    }

    public double cosine(double[] a, double[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    @SuppressWarnings("unchecked")
    private double[] remoteEmbed(String text) {
        Map<String, Object> resp = RestClient.create().post()
                .uri(baseUrl.trim() + "/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", embeddingModel, "input", text))
                .retrieve().body(Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.get("data");
        List<Number> vec = (List<Number>) data.get(0).get("embedding");
        double[] out = new double[vec.size()];
        for (int i = 0; i < vec.size(); i++) {
            out[i] = vec.get(i).doubleValue();
        }
        return out;
    }

    public static double[] localEmbed(String text) {
        double[] v = new double[LOCAL_DIM];
        for (String token : tokenize(text)) {
            v[Math.floorMod(token.hashCode(), LOCAL_DIM)] += 1.0;
        }
        double norm = 0;
        for (double x : v) norm += x * x;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
        return v;
    }

    static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        char prevCjk = 0;
        for (char c : text.toLowerCase().toCharArray()) {
            boolean cjk = c >= '\u4e00' && c <= '\u9fff';
            if (cjk) {
                if (word.length() > 0) {
                    tokens.add(word.toString());
                    word.setLength(0);
                }
                if (prevCjk != 0) tokens.add("" + prevCjk + c);
                tokens.add(String.valueOf(c));
                prevCjk = c;
            } else if (Character.isLetterOrDigit(c)) {
                word.append(c);
                prevCjk = 0;
            } else {
                if (word.length() > 0) {
                    tokens.add(word.toString());
                    word.setLength(0);
                }
                prevCjk = 0;
            }
        }
        if (word.length() > 0) tokens.add(word.toString());
        return tokens;
    }
}
