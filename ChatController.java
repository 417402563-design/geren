package cn.edu.sdua.geren.controller;

import cn.edu.sdua.geren.common.ApiResponse;
import cn.edu.sdua.geren.dto.Dtos;
import cn.edu.sdua.geren.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** 问答接口：conversationId 为空则新建对话，否则在既有对话上继续 */
    @PostMapping("/ask")
    public ApiResponse<Dtos.AskResp> ask(@RequestAttribute("userId") Long userId,
                                         @RequestBody Dtos.AskReq req) {
        return ApiResponse.ok(chatService.ask(userId, req));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<Dtos.ConversationVO>> conversations(@RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(chatService.listConversations(userId));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<Dtos.MessageVO>> messages(@PathVariable Long id,
                                                      @RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(chatService.listMessages(id, userId));
    }

    @DeleteMapping("/conversations/{id}")
    public ApiResponse<Void> deleteConversation(@PathVariable Long id,
                                                @RequestAttribute("userId") Long userId) {
        chatService.deleteConversation(id, userId);
        return ApiResponse.ok(null);
    }
}
