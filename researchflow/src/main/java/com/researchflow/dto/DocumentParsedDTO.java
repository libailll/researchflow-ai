package com.researchflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DocumentParsedDTO(
        @NotEmpty(message = "解析结果不能为空")
        @Size(max = 5000, message = "单个文档分块数不能超过 5000")
        List<@Valid DocumentChunkDTO> chunks
) {
}
