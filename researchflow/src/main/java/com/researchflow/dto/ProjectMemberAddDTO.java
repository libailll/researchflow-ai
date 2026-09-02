package com.researchflow.dto;

import com.researchflow.enums.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProjectMemberAddDTO {

    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID必须大于 0")
    private Long userId;

    @NotNull(message = "成员角色不能为空")
    private ProjectMemberRole role;
}
