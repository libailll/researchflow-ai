package com.researchflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record WeeklyReportGenerateDTO(
        @NotNull(message = "周报开始日期不能为空") LocalDate periodStart,
        @NotNull(message = "周报结束日期不能为空") LocalDate periodEnd,
        @Size(max = 160, message = "周报标题不能超过 160 个字符") String title
) {
}
