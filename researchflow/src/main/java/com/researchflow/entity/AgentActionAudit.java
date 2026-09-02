package com.researchflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_action_audit")
public class AgentActionAudit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long userId;
    private Long conversationId;
    private String actionType;
    private String targetType;
    private Long targetId;
    private String requestPayload;
    private String resultPayload;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
}
