package cn.edu.sdua.geren.dto;

import java.util.List;

/**
 * 各模块请求/响应 DTO 集中定义。
 */
public class Dtos {

    // ---------- 用户管理 ----------
    public record RegisterReq(String username, String password, String nickname) {}
    public record LoginReq(String username, String password) {}
    public record LoginResp(String token, UserVO user) {}
    public record UserVO(Long id, String username, String nickname, String email, String createdAt) {}
    public record UpdateProfileReq(String nickname, String email) {}
    public record ChangePasswordReq(String oldPassword, String newPassword) {}

    // ---------- 文档管理 ----------
    public record DocumentVO(Long id, String name, String fileType, Long fileSize, String status,
                             String statusText, Integer chunkCount, String errorMsg, String createdAt) {}

    // ---------- 智能问答 ----------
    public record AskReq(Long conversationId, String question) {}
    public record Citation(Long documentId, String documentName, Integer chunkIndex, String snippet, Double score) {}
    public record AskResp(Long conversationId, String answer, List<Citation> citations) {}

    // ---------- 对话历史 ----------
    public record ConversationVO(Long id, String title, String createdAt, String updatedAt) {}
    public record MessageVO(Long id, String role, String content, List<Citation> citations, String createdAt) {}
}
