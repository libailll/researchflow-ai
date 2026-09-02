package com.researchflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.context.UserContext;
import com.researchflow.entity.Project;
import com.researchflow.entity.ProjectMember;
import com.researchflow.enums.ProjectMemberRole;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.ProjectMapper;
import com.researchflow.mapper.ProjectMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectPermissionService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;

    public Project getProjectOrThrow(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    public Project requireAccess(Long projectId) {
        return requireAccess(projectId, UserContext.getUserId());
    }

    public Project requireAccess(Long projectId, Long userId) {
        Project project = getProjectOrThrow(projectId);
        if (!project.getOwnerId().equals(userId) && findMember(projectId, userId) == null) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
        return project;
    }

    public Project requireManager(Long projectId) {
        Project project = getProjectOrThrow(projectId);
        if (!canManage(project, UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
        return project;
    }

    public Project requireOwner(Long projectId) {
        Project project = getProjectOrThrow(projectId);
        if (!project.getOwnerId().equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
        return project;
    }

    public boolean canManage(Project project, Long userId) {
        if (project.getOwnerId().equals(userId)) {
            return true;
        }
        ProjectMember member = findMember(project.getId(), userId);
        return member != null && member.getRole() == ProjectMemberRole.ADMIN;
    }

    public ProjectMember findMember(Long projectId, Long userId) {
        return projectMemberMapper.selectOne(
                new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getProjectId, projectId)
                        .eq(ProjectMember::getUserId, userId)
        );
    }
}
