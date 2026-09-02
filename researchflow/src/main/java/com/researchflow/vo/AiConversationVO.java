package com.researchflow.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiConversationVO {
    private Long id;
    private Long projectId;
    private String title;
    private String lastMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
