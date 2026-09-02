package com.researchflow.service;

import com.researchflow.dto.ProjectCreateDTO;
import com.researchflow.dto.ProjectMemberAddDTO;
import com.researchflow.dto.ProjectUpdateDTO;
import com.researchflow.vo.ProjectMemberVO;
import com.researchflow.vo.ProjectVO;

import java.util.List;

public interface ProjectService {

    ProjectVO createProject(ProjectCreateDTO dto);

    List<ProjectVO> listProjects();

    ProjectVO getProject(Long projectId);

    ProjectVO updateProject(Long projectId, ProjectUpdateDTO dto);

    void deleteProject(Long projectId);

    ProjectMemberVO addMember(Long projectId, ProjectMemberAddDTO dto);

    List<ProjectMemberVO> listMembers(Long projectId);

    void removeMember(Long projectId, Long userId);
}
