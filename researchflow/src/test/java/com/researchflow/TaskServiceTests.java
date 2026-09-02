package com.researchflow;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.context.UserContext;
import com.researchflow.dto.TaskCreateDTO;
import com.researchflow.dto.TaskStatusUpdateDTO;
import com.researchflow.entity.Project;
import com.researchflow.entity.ProjectMember;
import com.researchflow.entity.Task;
import com.researchflow.enums.ProjectMemberRole;
import com.researchflow.enums.TaskPriority;
import com.researchflow.enums.TaskStatus;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.TaskMapper;
import com.researchflow.service.ProjectPermissionService;
import com.researchflow.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class TaskServiceTests {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @InjectMocks
    private TaskServiceImpl taskService;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void managerCanCreateTaskForProjectMember() {
        UserContext.setUserId(1L);
        Project project = project();
        when(projectPermissionService.requireManager(100L)).thenReturn(project);
        when(projectPermissionService.findMember(100L, 2L))
                .thenReturn(member(2L, ProjectMemberRole.MEMBER));
        when(taskMapper.insert(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(500L);
            return 1;
        });

        TaskCreateDTO dto = createDto(2L);
        var result = taskService.createTask(100L, dto);

        assertThat(result.getId()).isEqualTo(500L);
        assertThat(result.getProjectId()).isEqualTo(100L);
        assertThat(result.getCreatorId()).isEqualTo(1L);
        assertThat(result.getAssigneeId()).isEqualTo(2L);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(result.getProgress()).isZero();
    }

    @Test
    void taskCannotBeAssignedToNonMember() {
        UserContext.setUserId(1L);
        Project project = project();
        when(projectPermissionService.requireManager(100L)).thenReturn(project);
        when(projectPermissionService.findMember(100L, 9L)).thenReturn(null);

        assertThatThrownBy(() -> taskService.createTask(100L, createDto(9L)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.TASK_ASSIGNEE_NOT_PROJECT_MEMBER.getCode());
        verify(taskMapper, never()).insert(any(Task.class));
    }

    @Test
    void assigneeCanCompleteOwnTask() {
        UserContext.setUserId(2L);
        Project project = project();
        Task task = task(500L, 2L, TaskStatus.IN_PROGRESS, LocalDate.now().plusDays(1));
        when(taskMapper.selectById(500L)).thenReturn(task);
        when(projectPermissionService.requireAccess(100L)).thenReturn(project);
        when(projectPermissionService.canManage(project, 2L)).thenReturn(false);

        TaskStatusUpdateDTO dto = new TaskStatusUpdateDTO();
        dto.setStatus(TaskStatus.DONE);
        var result = taskService.updateStatus(500L, dto);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(result.getProgress()).isEqualTo(100);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(taskMapper).updateById(task);
    }

    @Test
    void ordinaryMemberCannotChangeSomeoneElsesTaskStatus() {
        UserContext.setUserId(3L);
        Project project = project();
        Task task = task(500L, 2L, TaskStatus.TODO, LocalDate.now().plusDays(1));
        when(taskMapper.selectById(500L)).thenReturn(task);
        when(projectPermissionService.requireAccess(100L)).thenReturn(project);
        when(projectPermissionService.canManage(project, 3L)).thenReturn(false);

        TaskStatusUpdateDTO dto = new TaskStatusUpdateDTO();
        dto.setStatus(TaskStatus.IN_PROGRESS);

        assertThatThrownBy(() -> taskService.updateStatus(500L, dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.TASK_ACCESS_DENIED.getCode());
        verify(taskMapper, never()).updateById(any(Task.class));
    }

    @Test
    void dashboardCalculatesTaskStatistics() {
        when(projectPermissionService.requireAccess(100L)).thenReturn(project());
        List<Task> tasks = List.of(
                task(1L, 2L, TaskStatus.DONE, LocalDate.now().minusDays(2)),
                task(2L, 2L, TaskStatus.IN_PROGRESS, LocalDate.now().minusDays(1)),
                task(3L, 3L, TaskStatus.REVIEW, LocalDate.now().plusDays(1)),
                task(4L, 3L, TaskStatus.TODO, LocalDate.now().minusDays(3))
        );
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(tasks);

        var dashboard = taskService.getDashboard(100L);

        assertThat(dashboard.getTotalTasks()).isEqualTo(4);
        assertThat(dashboard.getCompletedTasks()).isEqualTo(1);
        assertThat(dashboard.getInProgressTasks()).isEqualTo(2);
        assertThat(dashboard.getOverdueTasks()).isEqualTo(2);
        assertThat(dashboard.getProgress()).isEqualTo(25);
    }

    @Test
    void invalidTaskDatesAreRejected() {
        UserContext.setUserId(1L);
        when(projectPermissionService.requireManager(100L)).thenReturn(project());
        TaskCreateDTO dto = createDto(null);
        dto.setStartDate(LocalDate.of(2026, 9, 1));
        dto.setDueDate(LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> taskService.createTask(100L, dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.TASK_DATE_INVALID.getCode());
        verify(taskMapper, never()).insert(any(Task.class));
    }

    private Project project() {
        Project project = new Project();
        project.setId(100L);
        project.setOwnerId(1L);
        return project;
    }

    private ProjectMember member(Long userId, ProjectMemberRole role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(100L);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    private TaskCreateDTO createDto(Long assigneeId) {
        TaskCreateDTO dto = new TaskCreateDTO();
        dto.setTitle("完成数据标注");
        dto.setDescription("标注绝缘子数据集");
        dto.setAssigneeId(assigneeId);
        dto.setPriority(TaskPriority.HIGH);
        dto.setStartDate(LocalDate.now());
        dto.setDueDate(LocalDate.now().plusDays(7));
        return dto;
    }

    private Task task(
            Long id,
            Long assigneeId,
            TaskStatus status,
            LocalDate dueDate
    ) {
        Task task = new Task();
        task.setId(id);
        task.setProjectId(100L);
        task.setTitle("测试任务");
        task.setAssigneeId(assigneeId);
        task.setCreatorId(1L);
        task.setPriority(TaskPriority.MEDIUM);
        task.setStatus(status);
        task.setProgress(status == TaskStatus.DONE ? 100 : 20);
        task.setDueDate(dueDate);
        return task;
    }
}
