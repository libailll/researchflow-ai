package com.researchflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WeeklyReportUpdateDTO(
        @NotBlank(message = "周报标题不能为空")
        @Size(max = 160, message = "周报标题不能超过 160 个字符") String title,
        @NotBlank(message = "周报内容不能为空") String content
) {
}
