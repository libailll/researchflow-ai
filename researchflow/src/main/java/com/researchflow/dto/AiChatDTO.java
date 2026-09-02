package com.researchflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiChatDTO(
        @Positive(message = "会话ID必须大于 0") Long conversationId,
        @NotBlank(message = "问题不能为空")
        @Size(max = 4000, message = "问题不能超过 4000 个字符") String message,
        @Size(max = 20, message = "最多携带 20 条历史消息") List<@Valid AiChatMessageDTO> history
) {
    public List<AiChatMessageDTO> safeHistory() {
        return history == null ? List.of() : history;
    }
}
