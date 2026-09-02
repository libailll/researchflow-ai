package com.researchflow.controller;

import com.researchflow.common.Result;
import com.researchflow.dto.TaskAssigneeUpdateDTO;
import com.researchflow.dto.TaskCreateDTO;
import com.researchflow.dto.TaskStatusUpdateDTO;
import com.researchflow.dto.TaskUpdateDTO;
import com.researchflow.enums.TaskPriority;
import com.researchflow.enums.TaskStatus;
import com.researchflow.service.TaskService;
import com.researchflow.vo.ProjectDashboardVO;
import com.researchflow.vo.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "任务管理", description = "项目任务、状态、负责人和仪表盘")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    @Operation(summary = "创建项目任务")
    public Result<TaskVO> createTask(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @Valid @RequestBody TaskCreateDTO dto
    ) {
        return Result.success(taskService.createTask(projectId, dto));
    }

    @GetMapping("/projects/{projectId}/tasks")
    @Operation(summary = "查询项目任务", description = "支持状态、负责人、优先级和截止日期筛选")
    public Result<List<TaskVO>> listTasks(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @Positive(message = "负责人ID必须大于 0")
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) TaskPriority priority,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate deadline
    ) {
        return Result.success(
                taskService.listTasks(projectId, status, assigneeId, priority, deadline)
        );
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询任务详情")
    public Result<TaskVO> getTask(
            @Positive(message = "任务ID必须大于 0") @PathVariable Long taskId
    ) {
        return Result.success(taskService.getTask(taskId));
    }

    @PutMapping("/tasks/{taskId}")
    @Operation(summary = "修改任务")
    public Result<TaskVO> updateTask(
            @Positive(message = "任务ID必须大于 0") @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateDTO dto
    ) {
        return Result.success(taskService.updateTask(taskId, dto));
    }

    @PutMapping("/tasks/{taskId}/status")
    @Operation(summary = "修改任务状态")
    public Result<TaskVO> updateStatus(
            @Positive(message = "任务ID必须大于 0") @PathVariable Long taskId,
            @Valid @RequestBody TaskStatusUpdateDTO dto
    ) {
        return Result.success(taskService.updateStatus(taskId, dto));
    }

    @PutMapping("/tasks/{taskId}/assignee")
    @Operation(summary = "指派任务负责人")
    public Result<TaskVO> updateAssignee(
            @Positive(message = "任务ID必须大于 0") @PathVariable Long taskId,
            @Valid @RequestBody TaskAssigneeUpdateDTO dto
    ) {
        return Result.success(taskService.updateAssignee(taskId, dto));
    }

    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "删除任务")
    public Result<Void> deleteTask(
            @Positive(message = "任务ID必须大于 0") @PathVariable Long taskId
    ) {
        taskService.deleteTask(taskId);
        return Result.success();
    }

    @GetMapping("/projects/{projectId}/dashboard")
    @Operation(summary = "查询项目仪表盘")
    public Result<ProjectDashboardVO> getDashboard(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId
    ) {
        return Result.success(taskService.getDashboard(projectId));
    }
}
