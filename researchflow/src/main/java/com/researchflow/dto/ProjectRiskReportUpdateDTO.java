package com.researchflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRiskReportUpdateDTO(
        @NotBlank(message = "风险报告标题不能为空")
        @Size(max = 180, message = "风险报告标题不能超过 180 个字符") String title,
        @NotBlank(message = "风险报告内容不能为空") String content
) {
}
