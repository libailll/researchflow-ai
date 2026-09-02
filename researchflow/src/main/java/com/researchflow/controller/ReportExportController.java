package com.researchflow.controller;

import com.researchflow.service.ReportExportService;
import com.researchflow.vo.ExportedFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@Validated
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI 报告导出", description = "将已保存的 AI 报告导出为 PDF 或 Word")
@SecurityRequirement(name = "bearerAuth")
public class ReportExportController {

    private final ReportExportService reportExportService;

    @GetMapping("/weekly-reports/{reportId}/export")
    @Operation(summary = "导出项目周报")
    public ResponseEntity<byte[]> exportWeeklyReport(
            @Positive(message = "周报ID必须大于 0") @PathVariable Long reportId,
            @Parameter(description = "导出格式：pdf 或 docx")
            @RequestParam(defaultValue = "pdf") String format
    ) {
        return download(reportExportService.exportWeeklyReport(reportId, format));
    }

    @GetMapping("/document-summaries/{summaryId}/export")
    @Operation(summary = "导出文档 AI 总结")
    public ResponseEntity<byte[]> exportDocumentSummary(
            @Positive(message = "总结ID必须大于 0") @PathVariable Long summaryId,
            @Parameter(description = "导出格式：pdf 或 docx")
            @RequestParam(defaultValue = "pdf") String format
    ) {
        return download(reportExportService.exportDocumentSummary(summaryId, format));
    }

    @GetMapping("/risk-reports/{reportId}/export")
    @Operation(summary = "导出项目风险报告")
    public ResponseEntity<byte[]> exportRiskReport(
            @Positive(message = "风险报告ID必须大于 0") @PathVariable Long reportId,
            @Parameter(description = "导出格式：pdf 或 docx")
            @RequestParam(defaultValue = "pdf") String format
    ) {
        return download(reportExportService.exportRiskReport(reportId, format));
    }

    private ResponseEntity<byte[]> download(ExportedFile file) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.content().length)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }
}
