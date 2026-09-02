package com.researchflow.controller;

import com.researchflow.common.ErrorCode;
import com.researchflow.common.Result;
import com.researchflow.config.AiServiceProperties;
import com.researchflow.exception.BusinessException;
import com.researchflow.service.AgentToolService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/ai/projects/{projectId}/tools")
@RequiredArgsConstructor
public class InternalAgentController {

    private final AgentToolService agentToolService;
    private final AiServiceProperties aiServiceProperties;

    @GetMapping("/project")
    public Result<Map<String, Object>> getProject(
            @Positive @PathVariable Long projectId,
            @Positive @RequestParam Long userId,
            @RequestHeader("X-Internal-Token") String token
    ) {
        verifyToken(token);
        return Result.success(agentToolService.getProject(projectId, userId));
    }

    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> listTasks(
            @Positive @PathVariable Long projectId,
            @Positive @RequestParam Long userId,
            @RequestHeader("X-Internal-Token") String token
    ) {
        verifyToken(token);
        return Result.success(agentToolService.listTasks(projectId, userId));
    }

    @GetMapping("/tasks/overdue")
    public Result<List<Map<String, Object>>> getOverdueTasks(
            @Positive @PathVariable Long projectId,
            @Positive @RequestParam Long userId,
            @RequestHeader("X-Internal-Token") String token
    ) {
        verifyToken(token);
        return Result.success(agentToolService.getOverdueTasks(projectId, userId));
    }

    @GetMapping("/members")
    public Result<List<Map<String, Object>>> listMembers(
            @Positive @PathVariable Long projectId,
            @Positive @RequestParam Long userId,
            @RequestHeader("X-Internal-Token") String token
    ) {
        verifyToken(token);
        return Result.success(agentToolService.listMembers(projectId, userId));
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getProjectStatistics(
            @Positive @PathVariable Long projectId,
            @Positive @RequestParam Long userId,
            @RequestHeader("X-Internal-Token") String token
    ) {
        verifyToken(token);
        return Result.success(agentToolService.getProjectStatistics(projectId, userId));
    }

    private void verifyToken(String token) {
        boolean valid = MessageDigest.isEqual(
                aiServiceProperties.internalToken().getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8)
        );
        if (!valid) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVICE_UNAUTHORIZED);
        }
    }
}
