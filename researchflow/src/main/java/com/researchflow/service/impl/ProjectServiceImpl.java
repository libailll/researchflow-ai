package com.researchflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.context.UserContext;
import com.researchflow.dto.ProjectCreateDTO;
import com.researchflow.dto.ProjectMemberAddDTO;
import com.researchflow.dto.ProjectUpdateDTO;
import com.researchflow.entity.Project;
import com.researchflow.entity.ProjectMember;
import com.researchflow.entity.User;
import com.researchflow.enums.ProjectMemberRole;
import com.researchflow.enums.ProjectStatus;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.ProjectMapper;
import com.researchflow.mapper.ProjectMemberMapper;
import com.researchflow.mapper.UserMapper;
import com.researchflow.service.ProjectPermissionService;
import com.researchflow.service.ProjectProgressService;
import com.researchflow.service.ProjectService;
import com.researchflow.vo.ProjectMemberVO;
import com.researchflow.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectProgressService projectProgressService;

    @Override
    @Transactional
    public ProjectVO createProject(ProjectCreateDTO dto) {
        validateDates(dto.getStartDate(), dto.getEndDate());
        Long userId = UserContext.getUserId();

        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setOwnerId(userId);
        project.setStatus(ProjectStatus.PLANNING);
        project.setProgress(0);
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        LocalDateTime now = LocalDateTime.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        projectMapper.insert(project);

        ProjectMember owner = new ProjectMember();
        owner.setProjectId(project.getId());
        owner.setUserId(userId);
        owner.setRole(ProjectMemberRole.OWNER);
        owner.setJoinedAt(now);
        projectMemberMapper.insert(owner);

        log.info("Project created: projectId={}, ownerId={}", project.getId(), userId);
        return toProjectVO(project, 0);
    }

    @Override
    public List<ProjectVO> listProjects() {
        Long userId = UserContext.getUserId();
        List<ProjectMember> memberships = projectMemberMapper.selectList(
                new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getUserId, userId)
        );
        Set<Long> projectIds = memberships.stream()
                .map(ProjectMember::getProjectId)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (projectIds.isEmpty()) {
            wrapper.eq(Project::getOwnerId, userId);
        } else {
            wrapper.and(condition -> condition
                    .eq(Project::getOwnerId, userId)
                    .or()
                    .in(Project::getId, projectIds));
        }
        wrapper.orderByDesc(Project::getUpdatedAt);

        List<Project> projects = projectMapper.selectList(wrapper);
        Map<Long, Integer> progressByProject = projectProgressService.getProgress(
                projects.stream().map(Project::getId).toList()
        );
        return projects.stream()
                .map(project -> toProjectVO(project, progressByProject.getOrDefault(project.getId(), 0)))
                .toList();
    }

    @Override
    public ProjectVO getProject(Long projectId) {
        Project project = projectPermissionService.requireAccess(projectId);
        return toProjectVO(project, projectProgressService.getProgress(projectId));
    }

    @Override
    public ProjectVO updateProject(Long projectId, ProjectUpdateDTO dto) {
        validateDates(dto.getStartDate(), dto.getEndDate());
        Project project = projectPermissionService.requireManager(projectId);

        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setStatus(dto.getStatus());
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);

        log.info("Project updated: projectId={}, operatorId={}",
                projectId, UserContext.getUserId());
        return toProjectVO(project, projectProgressService.getProgress(projectId));
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId) {
        Project project = projectPermissionService.requireOwner(projectId);

        projectMemberMapper.delete(
                new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getProjectId, projectId)
        );
        projectMapper.deleteById(projectId);
        log.info("Project deleted: projectId={}, ownerId={}",
                projectId, UserContext.getUserId());
    }

    @Override
    @Transactional
    public ProjectMemberVO addMember(Long projectId, ProjectMemberAddDTO dto) {
        projectPermissionService.requireManager(projectId);

        if (dto.getRole() == ProjectMemberRole.OWNER) {
            throw new BusinessException(ErrorCode.INVALID_PROJECT_MEMBER_ROLE);
        }

        User user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (projectPermissionService.findMember(projectId, dto.getUserId()) != null) {
            throw new BusinessException(ErrorCode.PROJECT_MEMBER_EXISTS);
        }

        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(dto.getUserId());
        member.setRole(dto.getRole());
        member.setJoinedAt(LocalDateTime.now());
        projectMemberMapper.insert(member);

        log.info("Project member added: projectId={}, userId={}, role={}, operatorId={}",
                projectId, dto.getUserId(), dto.getRole(), UserContext.getUserId());
        return toProjectMemberVO(member, user);
    }

    @Override
    public List<ProjectMemberVO> listMembers(Long projectId) {
        projectPermissionService.requireAccess(projectId);

        List<ProjectMember> members = projectMemberMapper.selectList(
                new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getProjectId, projectId)
                        .orderByAsc(ProjectMember::getJoinedAt)
        );
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = members.stream()
                .map(ProjectMember::getUserId)
                .distinct()
                .toList();
        Map<Long, User> users = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return members.stream()
                .map(member -> toProjectMemberVO(member, users.get(member.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public void removeMember(Long projectId, Long userId) {
        projectPermissionService.requireManager(projectId);

        ProjectMember member = projectPermissionService.findMember(projectId, userId);
        if (member == null) {
            throw new BusinessException(ErrorCode.PROJECT_MEMBER_NOT_FOUND);
        }
        if (member.getRole() == ProjectMemberRole.OWNER) {
            throw new BusinessException(ErrorCode.CANNOT_REMOVE_PROJECT_OWNER);
        }

        projectMemberMapper.deleteById(member.getId());
        log.info("Project member removed: projectId={}, userId={}, operatorId={}",
                projectId, userId, UserContext.getUserId());
    }

    private void validateDates(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.PROJECT_DATE_INVALID);
        }
    }

    private ProjectVO toProjectVO(Project project, int progress) {
        ProjectVO vo = new ProjectVO();
        vo.setId(project.getId());
        vo.setName(project.getName());
        vo.setDescription(project.getDescription());
        vo.setOwnerId(project.getOwnerId());
        vo.setStatus(project.getStatus());
        vo.setProgress(progress);
        vo.setStartDate(project.getStartDate());
        vo.setEndDate(project.getEndDate());
        vo.setCreatedAt(project.getCreatedAt());
        vo.setUpdatedAt(project.getUpdatedAt());
        return vo;
    }

    private ProjectMemberVO toProjectMemberVO(ProjectMember member, User user) {
        ProjectMemberVO vo = new ProjectMemberVO();
        vo.setId(member.getId());
        vo.setProjectId(member.getProjectId());
        vo.setUserId(member.getUserId());
        vo.setRole(member.getRole());
        vo.setJoinedAt(member.getJoinedAt());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        return vo;
    }
}
