package com.researchflow.controller;

import com.researchflow.common.Result;
import com.researchflow.dto.AiConversationCreateDTO;
import com.researchflow.dto.AiConversationUpdateDTO;
import com.researchflow.service.AiConversationService;
import com.researchflow.vo.AiConversationDetailVO;
import com.researchflow.vo.AiConversationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "AI 会话", description = "AI 会话与消息历史管理")
@SecurityRequirement(name = "bearerAuth")
public class AiConversationController {

    private final AiConversationService conversationService;

    @PostMapping("/projects/{projectId}/ai/conversations")
    @Operation(summary = "创建 AI 会话")
    public Result<AiConversationVO> create(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @Valid @RequestBody AiConversationCreateDTO request
    ) {
        return Result.success(conversationService.create(projectId, request));
    }

    @GetMapping("/projects/{projectId}/ai/conversations")
    @Operation(summary = "查询当前用户的 AI 会话")
    public Result<List<AiConversationVO>> list(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId
    ) {
        return Result.success(conversationService.list(projectId));
    }

    @GetMapping("/ai/conversations/{conversationId}")
    @Operation(summary = "查询 AI 会话及消息")
    public Result<AiConversationDetailVO> detail(
            @Positive(message = "会话ID必须大于 0") @PathVariable Long conversationId
    ) {
        return Result.success(conversationService.detail(conversationId));
    }

    @PutMapping("/ai/conversations/{conversationId}")
    @Operation(summary = "重命名 AI 会话")
    public Result<AiConversationVO> rename(
            @Positive(message = "会话ID必须大于 0") @PathVariable Long conversationId,
            @Valid @RequestBody AiConversationUpdateDTO request
    ) {
        return Result.success(conversationService.rename(conversationId, request));
    }

    @DeleteMapping("/ai/conversations/{conversationId}")
    @Operation(summary = "删除 AI 会话")
    public Result<Void> delete(
            @Positive(message = "会话ID必须大于 0") @PathVariable Long conversationId
    ) {
        conversationService.delete(conversationId);
        return Result.success();
    }

    @DeleteMapping("/ai/conversations/{conversationId}/messages")
    @Operation(summary = "清空 AI 会话消息")
    public Result<Void> clearMessages(
            @Positive(message = "会话ID必须大于 0") @PathVariable Long conversationId
    ) {
        conversationService.clearMessages(conversationId);
        return Result.success();
    }
}
