package com.researchflow.controller;

import com.researchflow.common.Result;
import com.researchflow.dto.DocumentSummaryGenerateDTO;
import com.researchflow.dto.DocumentSummaryUpdateDTO;
import com.researchflow.service.DocumentSummaryService;
import com.researchflow.vo.DocumentSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "AI 文档总结", description = "生成、保存与管理单篇项目文档总结")
@SecurityRequirement(name = "bearerAuth")
public class DocumentSummaryController {

    private final DocumentSummaryService documentSummaryService;

    @PostMapping("/documents/{documentId}/ai/summary")
    @Operation(summary = "生成并保存文档 AI 总结")
    public Result<DocumentSummaryVO> generate(
            @Positive(message = "文档ID必须大于 0") @PathVariable Long documentId,
            @Valid @RequestBody DocumentSummaryGenerateDTO request
    ) {
        return Result.success(documentSummaryService.generate(documentId, request));
    }

    @GetMapping("/documents/{documentId}/ai/summaries")
    @Operation(summary = "查询文档 AI 总结历史")
    public Result<List<DocumentSummaryVO>> list(
            @Positive(message = "文档ID必须大于 0") @PathVariable Long documentId
    ) {
        return Result.success(documentSummaryService.list(documentId));
    }

    @GetMapping("/ai/document-summaries/{summaryId}")
    @Operation(summary = "查询文档 AI 总结详情")
    public Result<DocumentSummaryVO> detail(
            @Positive(message = "总结ID必须大于 0") @PathVariable Long summaryId
    ) {
        return Result.success(documentSummaryService.detail(summaryId));
    }

    @PutMapping("/ai/document-summaries/{summaryId}")
    @Operation(summary = "编辑已保存的文档总结")
    public Result<DocumentSummaryVO> update(
            @Positive(message = "总结ID必须大于 0") @PathVariable Long summaryId,
            @Valid @RequestBody DocumentSummaryUpdateDTO request
    ) {
        return Result.success(documentSummaryService.update(summaryId, request));
    }

    @DeleteMapping("/ai/document-summaries/{summaryId}")
    @Operation(summary = "删除文档总结")
    public Result<Void> delete(
            @Positive(message = "总结ID必须大于 0") @PathVariable Long summaryId
    ) {
        documentSummaryService.delete(summaryId);
        return Result.success();
    }
}
