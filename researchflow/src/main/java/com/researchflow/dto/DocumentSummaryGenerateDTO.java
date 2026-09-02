package com.researchflow.dto;

import jakarta.validation.constraints.Size;

public record DocumentSummaryGenerateDTO(
        @Size(max = 180, message = "总结标题不能超过 180 个字符") String title
) {
}
