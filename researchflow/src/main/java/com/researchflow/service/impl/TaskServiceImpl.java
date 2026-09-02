package com.researchflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.context.UserContext;
import com.researchflow.dto.TaskAssigneeUpdateDTO;
import com.researchflow.dto.TaskCreateDTO;
import com.researchflow.dto.TaskStatusUpdateDTO;
import com.researchflow.dto.TaskUpdateDTO;
import com.researchflow.entity.Project;
import com.researchflow.entity.ProjectMember;
import com.researchflow.entity.Task;
import com.researchflow.enums.TaskPriority;
import com.researchflow.enums.TaskStatus;
import com.researchflow.enums.NotificationType;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.TaskMapper;
import com.researchflow.mapper.ProjectMemberMapper;
import com.researchflow.service.ProjectPermissionService;
import com.researchflow.service.ProjectProgressService;
import com.researchflow.service.TaskService;
import com.researchflow.service.NotificationService;
import com.researchflow.vo.ProjectDashboardVO;
import com.researchflow.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectProgressService projectProgressService;
    private final NotificationService notificationService;
    private final ProjectMemberMapper projectMemberMapper;

    @Override
    @Transactional
    public TaskVO createTask(Long projectId, TaskCreateDTO dto) {
        Project project = projectPermissionService.requireManager(projectId);
        validateDates(dto.getStartDate(), dto.getDueDate());
        validateAssignee(project, dto.getAssigneeId());

        LocalDateTime now = LocalDateTime.now();
        Task task = new Task();
        task.setProjectId(projectId);
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setAssigneeId(dto.getAssigneeId());
        task.setCreatorId(UserContext.getUserId());
        task.setPriority(dto.getPriority());
        task.setStatus(TaskStatus.TODO);
        task.setProgress(0);
        task.setStartDate(dto.getStartDate());
        task.setDueDate(dto.getDueDate());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        taskMapper.insert(task);
        notificationService.taskAssigned(task, UserContext.getUserId());

        log.info("Task created: taskId={}, projectId={}, creatorId={}, assigneeId={}",
                task.getId(), projectId, task.getCreatorId(), task.getAssigneeId());
        return toTaskVO(task);
    }

    @Override
    public List<TaskVO> listTasks(
            Long projectId,
            TaskStatus status,
            Long assigneeId,
            TaskPriority priority,
            LocalDate deadline
    ) {
        projectPermissionService.requireAccess(projectId);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getProjectId, projectId)
                .eq(status != null, Task::getStatus, status)
                .eq(assigneeId != null, Task::getAssigneeId, assigneeId)
                .eq(priority != null, Task::getPriority, priority)
                .le(deadline != null, Task::getDueDate, deadline)
                .orderByAsc(Task::getDueDate)
                .orderByDesc(Task::getCreatedAt);
        return taskMapper.selectList(wrapper).stream()
                .map(this::toTaskVO)
                .toList();
    }

    @Override
    public TaskVO getTask(Long taskId) {
        Task task = getTaskOrThrow(taskId);
        projectPermissionService.requireAccess(task.getProjectId());
        return toTaskVO(task);
    }

    @Override
    @Transactional
    public TaskVO updateTask(Long taskId, TaskUpdateDTO dto) {
        Task task = getTaskOrThrow(taskId);
        Project project = projectPermissionService.requireManager(task.getProjectId());
        validateDates(dto.getStartDate(), dto.getDueDate());
        validateAssignee(project, dto.getAssigneeId());

        Long oldAssigneeId = task.getAssigneeId();
        LocalDate oldDueDate = task.getDueDate();

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setAssigneeId(dto.getAssigneeId());
        task.setPriority(dto.getPriority());
        task.setProgress(task.getStatus() == TaskStatus.DONE ? 100 : dto.getProgress());
        task.setStartDate(dto.getStartDate());
        task.setDueDate(dto.getDueDate());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        Long operatorId = UserContext.getUserId();
        if (!Objects.equals(oldAssigneeId, task.getAssigneeId())) {
            notificationService.taskChanged(task, operatorId, recipients(oldAssigneeId, task.getAssigneeId()),
                    NotificationType.TASK_ASSIGNED, "任务负责人已调整",
                    "任务“" + task.getTitle() + "”的负责人发生了变化。");
        }
        if (!Objects.equals(oldDueDate, task.getDueDate())) {
            String dueDate = task.getDueDate() == null ? "未设置" : task.getDueDate().toString();
            notificationService.taskChanged(task, operatorId, recipients(task.getCreatorId(), task.getAssigneeId()),
                    NotificationType.TASK_UPDATED, "任务截止日期已调整",
                    "任务“" + task.getTitle() + "”的新截止日期为 " + dueDate + "。");
        }
        if (Objects.equals(oldAssigneeId, task.getAssigneeId()) && Objects.equals(oldDueDate, task.getDueDate())) {
            notificationService.taskChanged(task, operatorId, recipients(task.getCreatorId(), task.getAssigneeId()),
                    NotificationType.TASK_UPDATED, "任务信息已更新",
                    "任务“" + task.getTitle() + "”的内容或进度已更新。");
        }

        log.info("Task updated: taskId={}, operatorId={}", taskId, UserContext.getUserId());
        return toTaskVO(task);
    }

    @Override
    @Transactional
    public TaskVO updateStatus(Long taskId, TaskStatusUpdateDTO dto) {
        Task task = getTaskOrThrow(taskId);
        Project project = projectPermissionService.requireAccess(task.getProjectId());
        Long userId = UserContext.getUserId();
        boolean assignee = userId.equals(task.getAssigneeId());
        if (!projectPermissionService.canManage(project, userId) && !assignee) {
            throw new BusinessException(ErrorCode.TASK_ACCESS_DENIED);
        }

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(dto.getStatus());
        if (dto.getStatus() == TaskStatus.DONE) {
            task.setProgress(100);
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setCompletedAt(null);
        }
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        notificationService.taskChanged(task, userId, projectMemberIds(project),
                NotificationType.TASK_STATUS_CHANGED, "任务状态已更新",
                "任务“" + task.getTitle() + "”已从“" + statusLabel(oldStatus) + "”变更为“" + statusLabel(dto.getStatus()) + "”。");

        log.info("Task status updated: taskId={}, status={}, operatorId={}",
                taskId, dto.getStatus(), userId);
        return toTaskVO(task);
    }

    @Override
    @Transactional
    public TaskVO updateAssignee(Long taskId, TaskAssigneeUpdateDTO dto) {
        Task task = getTaskOrThrow(taskId);
        Project project = projectPermissionService.requireManager(task.getProjectId());
        validateAssignee(project, dto.getAssigneeId());

        Long oldAssigneeId = task.getAssigneeId();
        task.setAssigneeId(dto.getAssigneeId());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        notificationService.taskChanged(task, UserContext.getUserId(), recipients(oldAssigneeId, dto.getAssigneeId()),
                NotificationType.TASK_ASSIGNED, "任务负责人已调整",
                "任务“" + task.getTitle() + "”的负责人发生了变化。");

        log.info("Task assignee updated: taskId={}, assigneeId={}, operatorId={}",
                taskId, dto.getAssigneeId(), UserContext.getUserId());
        return toTaskVO(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        Task task = getTaskOrThrow(taskId);
        projectPermissionService.requireManager(task.getProjectId());
        taskMapper.deleteById(taskId);
        log.info("Task deleted: taskId={}, projectId={}, operatorId={}",
                taskId, task.getProjectId(), UserContext.getUserId());
    }

    @Override
    public ProjectDashboardVO getDashboard(Long projectId) {
        projectPermissionService.requireAccess(projectId);
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().eq(Task::getProjectId, projectId)
        );

        long total = tasks.size();
        long completed = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();
        long inProgress = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS
                        || task.getStatus() == TaskStatus.REVIEW)
                .count();
        LocalDate today = LocalDate.now();
        long overdue = tasks.stream()
                .filter(task -> task.getDueDate() != null)
                .filter(task -> task.getDueDate().isBefore(today))
                .filter(task -> task.getStatus() != TaskStatus.DONE
                        && task.getStatus() != TaskStatus.CANCELLED)
                .count();
        int progress = projectProgressService.calculate(tasks);

        return new ProjectDashboardVO(total, completed, inProgress, overdue, progress);
    }

    private Task getTaskOrThrow(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }

    private void validateAssignee(Project project, Long assigneeId) {
        if (assigneeId == null) {
            return;
        }
        boolean owner = project.getOwnerId().equals(assigneeId);
        if (!owner && projectPermissionService.findMember(project.getId(), assigneeId) == null) {
            throw new BusinessException(ErrorCode.TASK_ASSIGNEE_NOT_PROJECT_MEMBER);
        }
    }

    private void validateDates(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.TASK_DATE_INVALID);
        }
    }

    private List<Long> recipients(Long... userIds) {
        List<Long> result = new ArrayList<>();
        for (Long userId : userIds) {
            if (userId != null && !result.contains(userId)) result.add(userId);
        }
        return result;
    }

    private List<Long> projectMemberIds(Project project) {
        List<Long> result = projectMemberMapper.selectList(
                        new LambdaQueryWrapper<ProjectMember>()
                                .eq(ProjectMember::getProjectId, project.getId()))
                .stream()
                .map(ProjectMember::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (project.getOwnerId() != null && !result.contains(project.getOwnerId())) {
            result.add(project.getOwnerId());
        }
        return result;
    }

    private String statusLabel(TaskStatus status) {
        return switch (status) {
            case TODO -> "待开始";
            case IN_PROGRESS -> "进行中";
            case REVIEW -> "待审核";
            case DONE -> "已完成";
            case CANCELLED -> "已取消";
        };
    }

    private TaskVO toTaskVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setProjectId(task.getProjectId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setAssigneeId(task.getAssigneeId());
        vo.setCreatorId(task.getCreatorId());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getProgress());
        vo.setStartDate(task.getStartDate());
        vo.setDueDate(task.getDueDate());
        vo.setCompletedAt(task.getCompletedAt());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        return vo;
    }
}
