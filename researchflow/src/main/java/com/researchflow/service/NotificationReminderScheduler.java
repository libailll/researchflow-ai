package com.researchflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.researchflow.entity.Project;
import com.researchflow.entity.Task;
import com.researchflow.enums.NotificationType;
import com.researchflow.enums.TaskStatus;
import com.researchflow.mapper.ProjectMapper;
import com.researchflow.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReminderScheduler {

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final NotificationService notificationService;

    @Scheduled(
            initialDelayString = "${notification.reminder-initial-delay-ms:20000}",
            fixedDelayString = "${notification.reminder-fixed-delay-ms:3600000}"
    )
    public void createTaskReminders() {
        LocalDate today = LocalDate.now();
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .notIn(Task::getStatus, TaskStatus.DONE, TaskStatus.CANCELLED)
                .isNotNull(Task::getDueDate)
                .le(Task::getDueDate, today.plusDays(1)));
        if (tasks.isEmpty()) return;
        Map<Long, Project> projects = projectMapper.selectByIds(
                        tasks.stream().map(Task::getProjectId).distinct().toList())
                .stream().collect(Collectors.toMap(Project::getId, Function.identity()));
        int createdCandidates = 0;
        for (Task task : tasks) {
            Project project = projects.get(task.getProjectId());
            if (project == null) continue;
            Set<Long> recipients = new LinkedHashSet<>();
            recipients.add(task.getAssigneeId());
            recipients.add(project.getOwnerId());
            recipients.remove(null);
            boolean overdue = task.getDueDate().isBefore(today);
            NotificationType type = overdue ? NotificationType.TASK_OVERDUE : NotificationType.TASK_DUE_SOON;
            String title = overdue ? "任务已经逾期" : "任务将在明天到期";
            String content = "项目“" + project.getName() + "”中的任务“" + task.getTitle() + "”截止日期为 " + task.getDueDate() + "。";
            String key = type + ":" + task.getId() + ":" + task.getDueDate();
            for (Long recipient : recipients) {
                notificationService.createOnce(recipient, task.getProjectId(), type, title, content,
                        "TASK", task.getId(), "/tasks?taskId=" + task.getId(), key);
                createdCandidates++;
            }
        }
        log.info("Task reminder scan completed: tasks={}, notificationCandidates={}", tasks.size(), createdCandidates);
    }
}
