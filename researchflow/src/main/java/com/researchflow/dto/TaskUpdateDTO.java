package com.researchflow.dto;

import com.researchflow.enums.TaskPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskUpdateDTO {

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 200, message = "任务标题长度不能超过 200 个字符")
    private String title;

    @Size(max = 5000, message = "任务描述长度不能超过 5000 个字符")
    private String description;

    @Positive(message = "负责人ID必须大于 0")
    private Long assigneeId;

    @NotNull(message = "任务优先级不能为空")
    private TaskPriority priority;

    @NotNull(message = "任务进度不能为空")
    @Min(value = 0, message = "任务进度不能小于 0")
    @Max(value = 100, message = "任务进度不能大于 100")
    private Integer progress;

    private LocalDate startDate;
    private LocalDate dueDate;
}
