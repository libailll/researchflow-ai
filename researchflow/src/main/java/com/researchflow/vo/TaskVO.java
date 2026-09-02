package com.researchflow.vo;

import com.researchflow.enums.TaskPriority;
import com.researchflow.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TaskVO {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private Long assigneeId;
    private Long creatorId;
    private TaskPriority priority;
    private TaskStatus status;
    private Integer progress;
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
