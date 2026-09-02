package com.researchflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SemanticSearchDTO(
        @NotBlank(message = "检索问题不能为空")
        @Size(max = 1000, message = "检索问题不能超过 1000 个字符") String query,
        @Min(value = 1, message = "返回数量不能小于 1")
        @Max(value = 20, message = "返回数量不能超过 20") Integer topK
) {
    public int safeTopK() {
        return topK == null ? 5 : topK;
    }
}
