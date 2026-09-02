package com.researchflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record DocumentChunkDTO(
        @PositiveOrZero(message = "页码不能小于 0") Integer pageNumber,
        @PositiveOrZero(message = "分块序号不能小于 0") Integer chunkIndex,
        @NotBlank(message = "分块内容不能为空")
        @Size(max = 10000, message = "单个分块内容不能超过 10000 个字符") String content
) {
}
