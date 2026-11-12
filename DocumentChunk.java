package cn.edu.sdua.geren.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文档分片（Chunk）：文本分割后的最小检索单元，embedding 以 JSON 数组字符串存储，实现向量与原文同库存储。
 */
@Entity
@Table(name = "kb_chunk")
@Getter
@Setter
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long documentId;

    /** 冗余所属用户，便于按用户隔离检索 */
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Lob
    @Column(nullable = false)
    private String content;

    /** 向量（JSON 数组字符串） */
    @Lob
    @Column(nullable = false)
    private String embedding;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
