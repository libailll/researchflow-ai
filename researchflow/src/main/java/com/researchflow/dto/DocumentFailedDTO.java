package com.researchflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentFailedDTO(
        @NotBlank(message = "失败原因不能为空")
        @Size(max = 2000, message = "失败原因不能超过 2000 个字符") String error
) {
}
