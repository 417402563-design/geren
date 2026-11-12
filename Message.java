package cn.edu.sdua.geren.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "kb_message")
@Getter
@Setter
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    /** user / assistant */
    @Column(nullable = false, length = 16)
    private String role;

    @Lob
    @Column(nullable = false)
    private String content;

    /** 引用溯源信息（JSON 数组字符串） */
    @Lob
    private String citations;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
