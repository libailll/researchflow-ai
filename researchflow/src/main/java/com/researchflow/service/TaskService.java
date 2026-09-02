package com.researchflow.service;

import com.researchflow.dto.TaskAssigneeUpdateDTO;
import com.researchflow.dto.TaskCreateDTO;
import com.researchflow.dto.TaskStatusUpdateDTO;
import com.researchflow.dto.TaskUpdateDTO;
import com.researchflow.enums.TaskPriority;
import com.researchflow.enums.TaskStatus;
import com.researchflow.vo.ProjectDashboardVO;
import com.researchflow.vo.TaskVO;

import java.time.LocalDate;
import java.util.List;

public interface TaskService {

    TaskVO createTask(Long projectId, TaskCreateDTO dto);

    List<TaskVO> listTasks(
            Long projectId,
            TaskStatus status,
            Long assigneeId,
            TaskPriority priority,
            LocalDate deadline
    );

    TaskVO getTask(Long taskId);

    TaskVO updateTask(Long taskId, TaskUpdateDTO dto);

    TaskVO updateStatus(Long taskId, TaskStatusUpdateDTO dto);

    TaskVO updateAssignee(Long taskId, TaskAssigneeUpdateDTO dto);

    void deleteTask(Long taskId);

    ProjectDashboardVO getDashboard(Long projectId);
}
