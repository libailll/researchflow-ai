package com.researchflow;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.context.UserContext;
import com.researchflow.entity.Project;
import com.researchflow.entity.ProjectMember;
import com.researchflow.enums.ProjectMemberRole;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.ProjectMapper;
import com.researchflow.mapper.ProjectMemberMapper;
import com.researchflow.service.ProjectPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectPermissionServiceTests {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @InjectMocks
    private ProjectPermissionService permissionService;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void ownerHasProjectAccess() {
        UserContext.setUserId(1L);
        Project project = project();
        when(projectMapper.selectById(100L)).thenReturn(project);

        assertThat(permissionService.requireAccess(100L)).isSameAs(project);
    }

    @Test
    void projectMemberHasProjectAccess() {
        UserContext.setUserId(2L);
        Project project = project();
        when(projectMapper.selectById(100L)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(Wrapper.class)))
                .thenReturn(member(ProjectMemberRole.MEMBER));

        assertThat(permissionService.requireAccess(100L)).isSameAs(project);
    }

    @Test
    void nonMemberIsDenied() {
        UserContext.setUserId(9L);
        when(projectMapper.selectById(100L)).thenReturn(project());
        when(projectMemberMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> permissionService.requireAccess(100L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED.getCode());
    }

    @Test
    void adminCanManageButOrdinaryMemberCannot() {
        UserContext.setUserId(2L);
        Project project = project();
        when(projectMapper.selectById(100L)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(Wrapper.class)))
                .thenReturn(member(ProjectMemberRole.ADMIN));
        assertThat(permissionService.requireManager(100L)).isSameAs(project);

        when(projectMemberMapper.selectOne(any(Wrapper.class)))
                .thenReturn(member(ProjectMemberRole.MEMBER));
        assertThatThrownBy(() -> permissionService.requireManager(100L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED.getCode());
    }

    private Project project() {
        Project project = new Project();
        project.setId(100L);
        project.setOwnerId(1L);
        return project;
    }

    private ProjectMember member(ProjectMemberRole role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(100L);
        member.setUserId(2L);
        member.setRole(role);
        return member;
    }
}
