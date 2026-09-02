package com.researchflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.context.UserContext;
import com.researchflow.dto.AgentActionExecuteDTO;
import com.researchflow.dto.TaskCreateDTO;
import com.researchflow.dto.TaskUpdateDTO;
import com.researchflow.enums.TaskPriority;
import com.researchflow.exception.BusinessException;
import com.researchflow.vo.AgentActionResultVO;
import com.researchflow.vo.TaskVO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentActionService {

    private static final String CREATE_TASK = "CREATE_TASK";
    private static final String UPDATE_TASK = "UPDATE_TASK";

    private final TaskService taskService;
    private final ProjectPermissionService projectPermissionService;
    private final AiConversationService conversationService;
    private final AgentActionAuditService auditService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AgentActionResultVO execute(Long projectId, AgentActionExecuteDTO request) {
        Long userId = UserContext.getUserId();
        projectPermissionService.requireManager(projectId);
        if (request.conversationId() != null) {
            conversationService.requireConversation(request.conversationId(), projectId, userId);
        }

        String actionType = request.actionType().trim().toUpperCase();
        if (!Set.of(CREATE_TASK, UPDATE_TASK).contains(actionType)) {
            throw new BusinessException(ErrorCode.AGENT_ACTION_INVALID);
        }
        String requestJson = toJson(request.payload());
        try {
            TaskVO task = switch (actionType) {
                case CREATE_TASK -> createTask(projectId, request.payload());
                case UPDATE_TASK -> updateTask(projectId, request.payload());
                default -> throw new BusinessException(ErrorCode.AGENT_ACTION_INVALID);
            };
            String resultJson = toJson(task);
            var audit = auditService.record(
                    projectId, userId, request.conversationId(), actionType, task.getId(),
                    requestJson, resultJson, "SUCCESS", null
            );
            log.info("Agent action executed: auditId={}, actionType={}, projectId={}, userId={}, targetId={}",
                    audit.getId(), actionType, projectId, userId, task.getId());
            return new AgentActionResultVO(audit.getId(), actionType, "SUCCESS", task);
        } catch (RuntimeException exception) {
            auditService.record(
                    projectId, userId, request.conversationId(), actionType, null,
                    requestJson, null, "FAILED", truncate(exception.getMessage())
            );
            log.warn("Agent action failed: actionType={}, projectId={}, userId={}, reason={}",
                    actionType, projectId, userId, exception.getMessage());
            throw exception;
        }
    }

    private TaskVO createTask(Long projectId, Map<String, Object> payload) {
        TaskCreateDTO dto = new TaskCreateDTO();
        dto.setTitle(text(payload, "title", null));
        dto.setDescription(text(payload, "description", null));
        dto.setAssigneeId(longValue(payload, "assigneeId", null));
        dto.setPriority(priority(payload, "priority", null));
        dto.setStartDate(date(payload, "startDate", null));
        dto.setDueDate(date(payload, "dueDate", null));
        validate(dto);
        return taskService.createTask(projectId, dto);
    }

    private TaskVO updateTask(Long projectId, Map<String, Object> payload) {
        Long taskId = longValue(payload, "taskId", null);
        if (taskId == null) {
            throw new BusinessException(ErrorCode.AGENT_ACTION_INVALID);
        }
        TaskVO existing = taskService.getTask(taskId);
        if (!projectId.equals(existing.getProjectId())) {
            throw new BusinessException(ErrorCode.TASK_ACCESS_DENIED);
        }
        TaskUpdateDTO dto = new TaskUpdateDTO();
        dto.setTitle(text(payload, "title", existing.getTitle()));
        dto.setDescription(text(payload, "description", existing.getDescription()));
        dto.setAssigneeId(payload.containsKey("assigneeId")
                ? longValue(payload, "assigneeId", null) : existing.getAssigneeId());
        dto.setPriority(priority(payload, "priority", existing.getPriority()));
        dto.setProgress(integer(payload, "progress", existing.getProgress()));
        dto.setStartDate(date(payload, "startDate", existing.getStartDate()));
        dto.setDueDate(date(payload, "dueDate", existing.getDueDate()));
        validate(dto);
        return taskService.updateTask(taskId, dto);
    }

    private <T> void validate(T dto) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    violations.iterator().next().getMessage());
        }
    }

    private String text(Map<String, Object> payload, String key, String fallback) {
        if (!payload.containsKey(key)) return fallback;
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private Long longValue(Map<String, Object> payload, String key, Long fallback) {
        if (!payload.containsKey(key) || payload.get(key) == null) return fallback;
        try { return Long.valueOf(String.valueOf(payload.get(key))); }
        catch (NumberFormatException exception) { throw new BusinessException(ErrorCode.AGENT_ACTION_INVALID); }
    }

    private Integer integer(Map<String, Object> payload, String key, Integer fallback) {
        if (!payload.containsKey(key) || payload.get(key) == null) return fallback;
        try { return Integer.valueOf(String.valueOf(payload.get(key))); }
        catch (NumberFormatException exception) { throw new BusinessException(ErrorCode.AGENT_ACTION_INVALID); }
    }

    private TaskPriority priority(Map<String, Object> payload, String key, TaskPriority fallback) {
        if (!payload.containsKey(key) || payload.get(key) == null) return fallback;
        try { return TaskPriority.valueOf(String.valueOf(payload.get(key)).toUpperCase()); }
        catch (IllegalArgumentException exception) { throw new BusinessException(ErrorCode.AGENT_ACTION_INVALID); }
    }

    private LocalDate date(Map<String, Object> payload, String key, LocalDate fallback) {
        if (!payload.containsKey(key)) return fallback;
        if (payload.get(key) == null || String.valueOf(payload.get(key)).isBlank()) return null;
        try { return LocalDate.parse(String.valueOf(payload.get(key))); }
        catch (DateTimeParseException exception) { throw new BusinessException(ErrorCode.AGENT_ACTION_INVALID); }
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new BusinessException(ErrorCode.AGENT_ACTION_INVALID); }
    }

    private String truncate(String message) {
        if (message == null) return "未知错误";
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
