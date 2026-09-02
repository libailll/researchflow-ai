package com.researchflow.dto;

import jakarta.validation.constraints.Size;

public record ProjectRiskReportGenerateDTO(
        @Size(max = 180, message = "风险报告标题不能超过 180 个字符") String title
) {
}
