package com.researchflow.vo;

import com.researchflow.enums.DocumentFileType;
import com.researchflow.enums.DocumentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentVO {
    private Long id;
    private Long projectId;
    private Long uploaderId;
    private String uploaderName;
    private String originalName;
    private DocumentFileType fileType;
    private Long fileSize;
    private DocumentStatus parseStatus;
    private DocumentStatus vectorStatus;
    private String parseError;
    private LocalDateTime parsedAt;
    private String vectorError;
    private LocalDateTime vectorizedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
