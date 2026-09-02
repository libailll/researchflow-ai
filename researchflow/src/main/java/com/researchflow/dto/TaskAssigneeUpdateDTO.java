package com.researchflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TaskAssigneeUpdateDTO {

    @NotNull(message = "负责人ID不能为空")
    @Positive(message = "负责人ID必须大于 0")
    private Long assigneeId;
}
