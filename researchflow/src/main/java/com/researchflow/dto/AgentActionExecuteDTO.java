package com.researchflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record AgentActionExecuteDTO(
        @NotBlank(message = "Agent操作类型不能为空")
        String actionType,
        @Positive(message = "会话ID必须大于 0")
        Long conversationId,
        @NotEmpty(message = "Agent操作参数不能为空")
        @Size(max = 20, message = "Agent操作参数过多")
        Map<String, Object> payload
) {
}
