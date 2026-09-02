package com.researchflow.vo;

public record AgentActionResultVO(
        Long auditId,
        String actionType,
        String status,
        TaskVO task
) {
}
