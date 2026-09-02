package com.researchflow.vo;

import com.researchflow.enums.AiMessageRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiPersistedMessageVO {
    private Long id;
    private AiMessageRole role;
    private String content;
    private String reasoning;
    private List<SemanticSearchResultVO> sources;
    private String model;
    private LocalDateTime createdAt;
}
