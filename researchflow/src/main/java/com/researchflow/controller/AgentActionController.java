package com.researchflow.controller;

import com.researchflow.common.Result;
import com.researchflow.dto.AgentActionExecuteDTO;
import com.researchflow.service.AgentActionService;
import com.researchflow.vo.AgentActionResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/projects/{projectId}/ai/actions")
@RequiredArgsConstructor
@Tag(name = "Agent 操作", description = "用户确认后执行 Agent 提议的业务写操作")
@SecurityRequirement(name = "bearerAuth")
public class AgentActionController {

    private final AgentActionService agentActionService;

    @PostMapping("/execute")
    @Operation(summary = "确认并执行 Agent 操作", description = "当前仅支持创建任务和修改任务，执行时重新校验项目管理权限并写入审计记录")
    public Result<AgentActionResultVO> execute(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @Valid @RequestBody AgentActionExecuteDTO request
    ) {
        return Result.success(agentActionService.execute(projectId, request));
    }
}
