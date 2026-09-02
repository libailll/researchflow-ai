package com.researchflow.vo;

import com.researchflow.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Long id;
    private Long projectId;
    private NotificationType type;
    private String title;
    private String content;
    private String targetType;
    private Long targetId;
    private String targetPath;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
