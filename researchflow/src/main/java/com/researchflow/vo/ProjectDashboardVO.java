package com.researchflow.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDashboardVO {
    private Long totalTasks;
    private Long completedTasks;
    private Long inProgressTasks;
    private Long overdueTasks;
    private Integer progress;
}
