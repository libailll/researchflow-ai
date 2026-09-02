package com.researchflow.dto;

import com.researchflow.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskStatusUpdateDTO {

    @NotNull(message = "任务状态不能为空")
    private TaskStatus status;
}
