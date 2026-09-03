package com.researchflow;

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
import com.researchflow.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTests {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @Mock
    private ProjectProgressService projectProgressService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void creatingProjectAlsoCreatesOwnerMembership() {
        UserContext.setUserId(1L);
        when(projectMapper.insert(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(100L);
            return 1;
        });

        ProjectCreateDTO dto = new ProjectCreateDTO();
        dto.setName("无人机巡检项目");
        dto.setDescription("测试项目");
        dto.setStartDate(LocalDate.of(2026, 8, 16));
        dto.setEndDate(LocalDate.of(2026, 12, 31));

        var result = projectService.createProject(dto);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getOwnerId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.PLANNING);
        assertThat(result.getProgress()).isZero();

        ArgumentCaptor<ProjectMember> memberCaptor =
                ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberMapper).insert(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getProjectId()).isEqualTo(100L);
        assertThat(memberCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(ProjectMemberRole.OWNER);
    }

    @Test
    void nonMemberCannotReadProject() {
        UserContext.setUserId(3L);
        Project project = project(100L, 1L);
        when(projectPermissionService.requireAccess(100L))
                .thenThrow(new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED));

        assertThatThrownBy(() -> projectService.getProject(100L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED.getCode());
    }

    @Test
    void adminCanUpdateProject() {
        UserContext.setUserId(2L);
        Project project = project(100L, 1L);
        when(projectPermissionService.requireManager(100L)).thenReturn(project);

        ProjectUpdateDTO dto = new ProjectUpdateDTO();
        dto.setName("更新后的项目");
        dto.setDescription("更新描述");
        dto.setStatus(ProjectStatus.RUNNING);
        dto.setStartDate(LocalDate.of(2026, 8, 16));
        dto.setEndDate(LocalDate.of(2026, 12, 31));
        when(projectProgressService.getProgress(100L)).thenReturn(30);

        var result = projectService.updateProject(100L, dto);

        assertThat(result.getName()).isEqualTo("更新后的项目");
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.RUNNING);
        assertThat(result.getProgress()).isEqualTo(30);
        verify(projectMapper).updateById(project);
    }

    @Test
    void ownerCanAddActiveMember() {
        UserContext.setUserId(1L);
        Project project = project(100L, 1L);
        User targetUser = user(2L, "member002", 1);
        when(projectPermissionService.requireManager(100L)).thenReturn(project);
        when(userMapper.selectById(2L)).thenReturn(targetUser);
        when(projectPermissionService.findMember(100L, 2L)).thenReturn(null);
        when(projectMemberMapper.insert(any(ProjectMember.class))).thenAnswer(invocation -> {
            ProjectMember member = invocation.getArgument(0);
            member.setId(20L);
            return 1;
        });

        ProjectMemberAddDTO dto = new ProjectMemberAddDTO();
        dto.setUserId(2L);
        dto.setRole(ProjectMemberRole.MEMBER);

        var result = projectService.addMember(100L, dto);

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getUsername()).isEqualTo("member002");
        assertThat(result.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
    }

    @Test
    void projectOwnerCannotBeRemoved() {
        UserContext.setUserId(1L);
        Project project = project(100L, 1L);
        ProjectMember owner = member(1L, 100L, 1L, ProjectMemberRole.OWNER);
        when(projectPermissionService.requireManager(100L)).thenReturn(project);
        when(projectPermissionService.findMember(100L, 1L)).thenReturn(owner);

        assertThatThrownBy(() -> projectService.removeMember(100L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.CANNOT_REMOVE_PROJECT_OWNER.getCode());
        verify(projectMemberMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void invalidProjectDatesAreRejected() {
        UserContext.setUserId(1L);
        ProjectCreateDTO dto = new ProjectCreateDTO();
        dto.setName("日期错误项目");
        dto.setStartDate(LocalDate.of(2026, 9, 1));
        dto.setEndDate(LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> projectService.createProject(dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.PROJECT_DATE_INVALID.getCode());
        verify(projectMapper, never()).insert(any(Project.class));
    }

    private Project project(Long id, Long ownerId) {
        Project project = new Project();
        project.setId(id);
        project.setName("测试项目");
        project.setOwnerId(ownerId);
        project.setStatus(ProjectStatus.PLANNING);
        project.setProgress(0);
        return project;
    }

    private ProjectMember member(
            Long id,
            Long projectId,
            Long userId,
            ProjectMemberRole role
    ) {
        ProjectMember member = new ProjectMember();
        member.setId(id);
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    private User user(Long id, String username, Integer status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(username);
        user.setStatus(status);
        return user;
    }
}
