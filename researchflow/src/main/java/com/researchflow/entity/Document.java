package com.researchflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.researchflow.enums.DocumentFileType;
import com.researchflow.enums.DocumentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long uploaderId;
    private String fileName;
    private String originalName;
    private DocumentFileType fileType;
    private Long fileSize;
    private String storagePath;
    private DocumentStatus parseStatus;
    private DocumentStatus vectorStatus;
    private String parseError;
    private LocalDateTime parsedAt;
    private String vectorError;
    private LocalDateTime vectorizedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
