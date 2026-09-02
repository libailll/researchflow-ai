package com.researchflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.researchflow.entity.Task;
import com.researchflow.enums.TaskStatus;
import com.researchflow.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectProgressService {

    private final TaskMapper taskMapper;

    public int getProgress(Long projectId) {
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().eq(Task::getProjectId, projectId)
        );
        return calculate(tasks);
    }

    public Map<Long, Integer> getProgress(Collection<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return taskMapper.selectList(
                        new LambdaQueryWrapper<Task>().in(Task::getProjectId, projectIds)
                ).stream()
                .collect(Collectors.groupingBy(Task::getProjectId))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> calculate(entry.getValue())));
    }

    public int calculate(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return 0;
        }
        long completed = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();
        return (int) (completed * 100 / tasks.size());
    }
}
