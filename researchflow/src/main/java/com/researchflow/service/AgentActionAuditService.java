package com.researchflow.service;

import com.researchflow.entity.AgentActionAudit;
import com.researchflow.mapper.AgentActionAuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgentActionAuditService {

    private final AgentActionAuditMapper auditMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgentActionAudit record(Long projectId, Long userId, Long conversationId,
                                   String actionType, Long targetId, String requestPayload,
                                   String resultPayload, String status, String errorMessage) {
        AgentActionAudit audit = new AgentActionAudit();
        audit.setProjectId(projectId);
        audit.setUserId(userId);
        audit.setConversationId(conversationId);
        audit.setActionType(actionType);
        audit.setTargetType("TASK");
        audit.setTargetId(targetId);
        audit.setRequestPayload(requestPayload);
        audit.setResultPayload(resultPayload);
        audit.setStatus(status);
        audit.setErrorMessage(errorMessage);
        LocalDateTime now = LocalDateTime.now();
        audit.setCreatedAt(now);
        audit.setExecutedAt(now);
        auditMapper.insert(audit);
        return audit;
    }
}
