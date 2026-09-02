package com.researchflow.controller;

import com.researchflow.common.Result;
import com.researchflow.dto.ProjectCreateDTO;
import com.researchflow.dto.ProjectMemberAddDTO;
import com.researchflow.dto.ProjectUpdateDTO;
import com.researchflow.service.ProjectService;
import com.researchflow.vo.ProjectMemberVO;
import com.researchflow.vo.ProjectVO;
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
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "项目管理", description = "项目及项目成员管理")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "创建项目")
    public Result<ProjectVO> createProject(@Valid @RequestBody ProjectCreateDTO dto) {
        return Result.success(projectService.createProject(dto));
    }

    @GetMapping
    @Operation(summary = "查询当前用户参与的项目")
    public Result<List<ProjectVO>> listProjects() {
        return Result.success(projectService.listProjects());
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "查询项目详情")
    public Result<ProjectVO> getProject(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId
    ) {
        return Result.success(projectService.getProject(projectId));
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "修改项目")
    public Result<ProjectVO> updateProject(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateDTO dto
    ) {
        return Result.success(projectService.updateProject(projectId, dto));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "删除项目（仅所有者）")
    public Result<Void> deleteProject(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId
    ) {
        projectService.deleteProject(projectId);
        return Result.success();
    }

    @PostMapping("/{projectId}/members")
    @Operation(summary = "添加项目成员")
    public Result<ProjectMemberVO> addMember(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @Valid @RequestBody ProjectMemberAddDTO dto
    ) {
        return Result.success(projectService.addMember(projectId, dto));
    }

    @GetMapping("/{projectId}/members")
    @Operation(summary = "查询项目成员")
    public Result<List<ProjectMemberVO>> listMembers(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId
    ) {
        return Result.success(projectService.listMembers(projectId));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @Operation(summary = "移除项目成员")
    public Result<Void> removeMember(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @Positive(message = "用户ID必须大于 0") @PathVariable Long userId
    ) {
        projectService.removeMember(projectId, userId);
        return Result.success();
    }
}
