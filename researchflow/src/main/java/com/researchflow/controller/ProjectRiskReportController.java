package com.researchflow.controller;

import com.researchflow.common.Result;
import com.researchflow.dto.ProjectRiskReportGenerateDTO;
import com.researchflow.dto.ProjectRiskReportUpdateDTO;
import com.researchflow.service.ProjectRiskReportService;
import com.researchflow.vo.ProjectRiskReportVO;
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
@Tag(name = "AI 项目风险分析", description = "生成、保存与管理可解释的项目风险报告")
@SecurityRequirement(name = "bearerAuth")
public class ProjectRiskReportController {

    private final ProjectRiskReportService riskReportService;

    @PostMapping("/projects/{projectId}/ai/risk-report")
    @Operation(summary = "综合项目任务、周期、成员负载和知识库生成风险报告")
    public Result<ProjectRiskReportVO> generate(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @Valid @RequestBody ProjectRiskReportGenerateDTO request
    ) {
        return Result.success(riskReportService.generate(projectId, request));
    }

    @GetMapping("/projects/{projectId}/ai/risk-reports")
    @Operation(summary = "查询项目风险报告列表")
    public Result<List<ProjectRiskReportVO>> list(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId
    ) {
        return Result.success(riskReportService.list(projectId));
    }

    @GetMapping("/ai/risk-reports/{reportId}")
    @Operation(summary = "查询项目风险报告详情")
    public Result<ProjectRiskReportVO> detail(
            @Positive(message = "风险报告ID必须大于 0") @PathVariable Long reportId
    ) {
        return Result.success(riskReportService.detail(reportId));
    }

    @PutMapping("/ai/risk-reports/{reportId}")
    @Operation(summary = "编辑已保存的项目风险报告")
    public Result<ProjectRiskReportVO> update(
            @Positive(message = "风险报告ID必须大于 0") @PathVariable Long reportId,
            @Valid @RequestBody ProjectRiskReportUpdateDTO request
    ) {
        return Result.success(riskReportService.update(reportId, request));
    }

    @DeleteMapping("/ai/risk-reports/{reportId}")
    @Operation(summary = "删除项目风险报告")
    public Result<Void> delete(
            @Positive(message = "风险报告ID必须大于 0") @PathVariable Long reportId
    ) {
        riskReportService.delete(reportId);
        return Result.success();
    }
}
