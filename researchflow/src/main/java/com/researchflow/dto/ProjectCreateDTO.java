package com.researchflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectCreateDTO {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称长度不能超过 100 个字符")
    private String name;

    @Size(max = 2000, message = "项目描述长度不能超过 2000 个字符")
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;
}
