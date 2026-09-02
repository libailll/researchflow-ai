package com.researchflow.controller;

import com.researchflow.common.Result;
import com.researchflow.service.DocumentService;
import com.researchflow.service.DocumentProcessingService;
import com.researchflow.vo.DocumentChunkPageVO;
import com.researchflow.vo.DocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "项目文档上传、列表、下载与删除")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentProcessingService documentProcessingService;

    @PostMapping(value = "/projects/{projectId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传项目文档", description = "支持 PDF、DOCX、TXT 和 Markdown，上传后发送 document.parse 消息")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(type = "object")))
    public Result<DocumentVO> upload(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @Parameter(description = "文档文件", required = true)
            @RequestPart("file") MultipartFile file
    ) {
        return Result.success(documentService.upload(projectId, file));
    }

    @GetMapping("/projects/{projectId}/documents")
    @Operation(summary = "查询项目文档")
    public Result<List<DocumentVO>> list(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId
    ) {
        return Result.success(documentService.list(projectId));
    }

    @GetMapping("/documents/{documentId}")
    @Operation(summary = "查询文档详情")
    public Result<DocumentVO> get(
            @Positive(message = "文档ID必须大于 0") @PathVariable Long documentId
    ) {
        return Result.success(documentService.get(documentId));
    }

    @GetMapping("/documents/{documentId}/chunks")
    @Operation(summary = "分页查询文档解析片段")
    public Result<DocumentChunkPageVO> chunks(
            @Positive(message = "文档ID必须大于 0") @PathVariable Long documentId,
            @RequestParam(defaultValue = "1") @Positive(message = "页码必须大于 0") long page,
            @RequestParam(defaultValue = "20") @Positive(message = "每页数量必须大于 0") long size
    ) {
        long safeSize = Math.min(size, 100);
        return Result.success(documentProcessingService.listChunks(documentId, page, safeSize));
    }

    @GetMapping("/documents/{documentId}/download")
    @Operation(summary = "下载文档")
    public ResponseEntity<Resource> download(
            @Positive(message = "文档ID必须大于 0") @PathVariable Long documentId
    ) {
        DocumentService.DocumentDownload download = documentService.download(documentId);
        String disposition = ContentDisposition.attachment()
                .filename(download.originalName(), StandardCharsets.UTF_8)
                .build()
                .toString();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(download.resource());
    }

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "删除文档")
    public Result<Void> delete(
            @Positive(message = "文档ID必须大于 0") @PathVariable Long documentId
    ) {
        documentService.delete(documentId);
        return Result.success();
    }

    @PostMapping("/documents/{documentId}/vectorize")
    @Operation(summary = "重新提交文档向量化任务")
    public Result<Void> vectorize(
            @Positive(message = "文档ID必须大于 0") @PathVariable Long documentId
    ) {
        documentProcessingService.retryVectorize(documentId);
        return Result.success();
    }
}
