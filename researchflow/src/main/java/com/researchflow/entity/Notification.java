package com.researchflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.researchflow.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long projectId;
    private NotificationType type;
    private String title;
    private String content;
    private String targetType;
    private Long targetId;
    private String targetPath;
    private String businessKey;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
