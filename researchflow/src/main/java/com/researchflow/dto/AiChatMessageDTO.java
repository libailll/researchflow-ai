package com.researchflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiChatMessageDTO(
        @Pattern(regexp = "user|assistant", message = "消息角色不合法") String role,
        @NotBlank(message = "历史消息内容不能为空")
        @Size(max = 12000, message = "历史消息内容不能超过 12000 个字符") String content
) {
}
