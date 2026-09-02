package com.researchflow.vo;

import com.researchflow.enums.ProjectStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProjectVO {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private ProjectStatus status;
    private Integer progress;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
