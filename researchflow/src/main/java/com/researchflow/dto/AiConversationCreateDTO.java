package com.researchflow.dto;

import jakarta.validation.constraints.Size;

public record AiConversationCreateDTO(
        @Size(max = 120, message = "会话标题不能超过 120 个字符") String title
) {
}
