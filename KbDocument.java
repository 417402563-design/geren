package cn.edu.sdua.geren.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识库文档：状态流转 PARSING -> VECTORIZING -> READY / FAILED
 */
@Entity
@Table(name = "kb_document")
@Getter
@Setter
public class KbDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户（权限控制依据） */
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 16)
    private String fileType;

    private Long fileSize;

    @Column(nullable = false, length = 16)
    private String status;

    private Integer chunkCount;

    @Column(length = 512)
    private String errorMsg;

    /** 原始文件存储路径 */
    @Column(length = 512)
    private String filePath;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
