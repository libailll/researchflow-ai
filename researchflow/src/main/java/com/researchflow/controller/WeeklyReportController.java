package com.researchflow.controller;

import com.researchflow.common.Result;
import com.researchflow.dto.WeeklyReportGenerateDTO;
import com.researchflow.dto.WeeklyReportUpdateDTO;
import com.researchflow.service.WeeklyReportService;
import com.researchflow.vo.WeeklyReportVO;
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
@Tag(name = "AI 项目周报", description = "生成、保存与管理项目周报")
@SecurityRequirement(name = "bearerAuth")
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @PostMapping("/projects/{projectId}/ai/weekly-report")
    @Operation(summary = "读取项目数据与知识库并生成周报")
    public Result<WeeklyReportVO> generate(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @Valid @RequestBody WeeklyReportGenerateDTO request
    ) {
        return Result.success(weeklyReportService.generate(projectId, request));
    }

    @GetMapping("/projects/{projectId}/ai/weekly-reports")
    @Operation(summary = "查询项目周报列表")
    public Result<List<WeeklyReportVO>> list(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId
    ) {
        return Result.success(weeklyReportService.list(projectId));
    }

    @GetMapping("/ai/weekly-reports/{reportId}")
    @Operation(summary = "查询项目周报详情")
    public Result<WeeklyReportVO> detail(
            @Positive(message = "周报ID必须大于 0") @PathVariable Long reportId
    ) {
        return Result.success(weeklyReportService.detail(reportId));
    }

    @PutMapping("/ai/weekly-reports/{reportId}")
    @Operation(summary = "编辑已保存的项目周报")
    public Result<WeeklyReportVO> update(
            @Positive(message = "周报ID必须大于 0") @PathVariable Long reportId,
            @Valid @RequestBody WeeklyReportUpdateDTO request
    ) {
        return Result.success(weeklyReportService.update(reportId, request));
    }

    @DeleteMapping("/ai/weekly-reports/{reportId}")
    @Operation(summary = "删除项目周报")
    public Result<Void> delete(
            @Positive(message = "周报ID必须大于 0") @PathVariable Long reportId
    ) {
        weeklyReportService.delete(reportId);
        return Result.success();
    }
}
