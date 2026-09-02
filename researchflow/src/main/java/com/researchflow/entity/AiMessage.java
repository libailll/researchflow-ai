package com.researchflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.researchflow.enums.AiMessageRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_message")
public class AiMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private AiMessageRole role;
    private String content;
    private String reasoning;
    private String sources;
    private String model;
    private LocalDateTime createdAt;
}
