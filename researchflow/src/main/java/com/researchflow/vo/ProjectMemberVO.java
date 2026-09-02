package com.researchflow.vo;

import com.researchflow.enums.ProjectMemberRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectMemberVO {
    private Long id;
    private Long projectId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private ProjectMemberRole role;
    private LocalDateTime joinedAt;
}
