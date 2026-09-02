package com.researchflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.researchflow.entity.Project;
import com.researchflow.entity.ProjectMember;
import com.researchflow.entity.Task;
import com.researchflow.entity.User;
import com.researchflow.enums.TaskStatus;
import com.researchflow.mapper.ProjectMemberMapper;
import com.researchflow.mapper.TaskMapper;
import com.researchflow.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentToolService {

    private final ProjectPermissionService projectPermissionService;
    private final TaskMapper taskMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;
    private final ProjectProgressService projectProgressService;

    public Map<String, Object> getProject(Long projectId, Long userId) {
        Project project = projectPermissionService.requireAccess(projectId, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", project.getId());
        result.put("name", project.getName());
        result.put("description", project.getDescription());
        result.put("status", project.getStatus());
        result.put("progress", projectProgressService.getProgress(projectId));
        result.put("ownerId", project.getOwnerId());
        result.put("startDate", project.getStartDate());
        result.put("endDate", project.getEndDate());
        return result;
    }

    public List<Map<String, Object>> listTasks(Long projectId, Long userId) {
        projectPermissionService.requireAccess(projectId, userId);
        return findTasks(projectId).stream().map(this::toTaskData).toList();
    }

    public List<Map<String, Object>> getOverdueTasks(Long projectId, Long userId) {
        projectPermissionService.requireAccess(projectId, userId);
        LocalDate today = LocalDate.now();
        return findTasks(projectId).stream()
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(today))
                .filter(task -> task.getStatus() != TaskStatus.DONE
                        && task.getStatus() != TaskStatus.CANCELLED)
                .map(this::toTaskData)
                .toList();
    }

    public List<Map<String, Object>> listMembers(Long projectId, Long userId) {
        projectPermissionService.requireAccess(projectId, userId);
        List<ProjectMember> members = projectMemberMapper.selectList(
                new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getProjectId, projectId)
                        .orderByAsc(ProjectMember::getJoinedAt)
        );
        if (members.isEmpty()) return List.of();
        Map<Long, User> users = userMapper.selectByIds(
                        members.stream().map(ProjectMember::getUserId).distinct().toList()
                ).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return members.stream().map(member -> {
            User user = users.get(member.getUserId());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", member.getUserId());
            result.put("username", user == null ? null : user.getUsername());
            result.put("nickname", user == null ? null : user.getNickname());
            result.put("role", member.getRole());
            return result;
        }).toList();
    }

    public Map<String, Object> getProjectStatistics(Long projectId, Long userId) {
        Project project = projectPermissionService.requireAccess(projectId, userId);
        List<Task> tasks = findTasks(projectId);
        long completed = tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        long inProgress = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS
                        || task.getStatus() == TaskStatus.REVIEW)
                .count();
        LocalDate today = LocalDate.now();
        long overdue = tasks.stream()
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(today))
                .filter(task -> task.getStatus() != TaskStatus.DONE
                        && task.getStatus() != TaskStatus.CANCELLED)
                .count();
        int taskProgress = projectProgressService.calculate(tasks);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", projectId);
        result.put("projectName", project.getName());
        result.put("projectStatus", project.getStatus());
        result.put("projectProgress", taskProgress);
        result.put("totalTasks", tasks.size());
        result.put("completedTasks", completed);
        result.put("inProgressTasks", inProgress);
        result.put("overdueTasks", overdue);
        result.put("taskCompletionRate", taskProgress);
        return result;
    }

    private List<Task> findTasks(Long projectId) {
        return taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getProjectId, projectId)
                .orderByAsc(Task::getDueDate)
                .orderByDesc(Task::getCreatedAt));
    }

    private Map<String, Object> toTaskData(Task task) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", task.getId());
        result.put("title", task.getTitle());
        result.put("description", task.getDescription());
        result.put("status", task.getStatus());
        result.put("priority", task.getPriority());
        result.put("progress", task.getProgress());
        result.put("assigneeId", task.getAssigneeId());
        result.put("startDate", task.getStartDate());
        result.put("dueDate", task.getDueDate());
        return result;
    }
}
