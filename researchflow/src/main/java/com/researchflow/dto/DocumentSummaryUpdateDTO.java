package com.researchflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentSummaryUpdateDTO(
        @NotBlank(message = "总结标题不能为空")
        @Size(max = 180, message = "总结标题不能超过 180 个字符") String title,
        @NotBlank(message = "总结内容不能为空") String content
) {
}
