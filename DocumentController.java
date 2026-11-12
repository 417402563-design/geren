package cn.edu.sdua.geren.controller;

import cn.edu.sdua.geren.common.ApiResponse;
import cn.edu.sdua.geren.dto.Dtos;
import cn.edu.sdua.geren.service.DocumentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ApiResponse<Dtos.DocumentVO> upload(@RequestParam("file") MultipartFile file,
                                               @RequestAttribute("userId") Long userId) throws Exception {
        return ApiResponse.ok(documentService.upload(file, userId));
    }

    @GetMapping
    public ApiResponse<List<Dtos.DocumentVO>> list(@RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(documentService.list(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Dtos.DocumentVO> detail(@PathVariable Long id, @RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(documentService.detail(id, userId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestAttribute("userId") Long userId) throws Exception {
        documentService.delete(id, userId);
        return ApiResponse.ok(null);
    }
}
