package com.researchflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiConversationUpdateDTO(
        @NotBlank(message = "会话标题不能为空")
        @Size(max = 120, message = "会话标题不能超过 120 个字符") String title
) {
}
