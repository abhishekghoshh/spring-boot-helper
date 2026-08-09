package com.loansphere.document.controller;

import com.loansphere.common.api.ApiResponse;
import com.loansphere.common.security.CurrentUser;
import com.loansphere.document.model.DocumentMetadata;
import com.loansphere.document.service.DocumentService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentMetadata>> upload(
            @CurrentUser String userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType) {
        DocumentMetadata doc = documentService.upload(userId, file, documentType);
        return ResponseEntity.ok(ApiResponse.success("Document uploaded", doc));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentMetadata>>> getUserDocuments(@CurrentUser String userId) {
        return ResponseEntity.ok(ApiResponse.success(documentService.getUserDocuments(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentMetadata>> getDocument(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(documentService.getDocument(id)));
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable String id, HttpServletResponse response) throws IOException {
        DocumentMetadata doc = documentService.getDocument(id);
        response.setContentType(doc.getContentType());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + doc.getFileName() + "\"");
        documentService.download(id, response.getOutputStream());
        response.flushBuffer();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Document deleted", null));
    }
}
