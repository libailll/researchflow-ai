package com.researchflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.context.UserContext;
import com.researchflow.dto.ProjectRiskReportGenerateDTO;
import com.researchflow.dto.ProjectRiskReportUpdateDTO;
import com.researchflow.entity.Document;
import com.researchflow.entity.Project;
import com.researchflow.entity.ProjectMember;
import com.researchflow.entity.ProjectRiskReport;
import com.researchflow.entity.Task;
import com.researchflow.entity.User;
import com.researchflow.enums.DocumentStatus;
import com.researchflow.enums.ProjectRiskLevel;
import com.researchflow.enums.TaskPriority;
import com.researchflow.enums.TaskStatus;
import com.researchflow.enums.NotificationType;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.DocumentMapper;
import com.researchflow.mapper.ProjectMemberMapper;
import com.researchflow.mapper.ProjectRiskReportMapper;
import com.researchflow.mapper.TaskMapper;
import com.researchflow.mapper.UserMapper;
import com.researchflow.vo.ProjectRiskReportAiResultVO;
import com.researchflow.vo.ProjectRiskReportVO;
import com.researchflow.vo.SemanticSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectRiskReportService {

    private final ProjectRiskReportMapper riskReportMapper;
    private final TaskMapper taskMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final DocumentMapper documentMapper;
    private final UserMapper userMapper;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectProgressService projectProgressService;
    private final AiServiceClient aiServiceClient;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProjectRiskReportVO generate(Long projectId, ProjectRiskReportGenerateDTO request) {
        Project project = projectPermissionService.requireAccess(projectId);
        Long userId = UserContext.getUserId();
        RiskContext riskContext = buildContext(project, userId);
        ProjectRiskReportAiResultVO generated = aiServiceClient.generateProjectRiskReport(riskContext.payload());
        if (generated.content() == null || generated.content().isBlank()) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        ProjectRiskReport report = new ProjectRiskReport();
        report.setProjectId(projectId);
        report.setCreatorId(userId);
        report.setTitle(normalizeTitle(request.title(), project.getName()));
        report.setRiskLevel(riskContext.level());
        report.setRiskScore(riskContext.score());
        report.setContent(generated.content().trim());
        report.setAnalysisSnapshot(writeJson(riskContext.snapshot(), "risk analysis snapshot"));
        report.setSources(writeJson(generated.sources(), "risk report sources"));
        report.setModel(generated.model());
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        riskReportMapper.insert(report);
        notificationService.aiResultReady(userId, projectId, NotificationType.RISK_REPORT_READY,
                "项目风险分析已生成", "“" + report.getTitle() + "”已经生成，当前风险等级为 " + report.getRiskLevel() + "。",
                "RISK_REPORT", report.getId(), "/risk-analysis?reportId=" + report.getId());
        log.info("AI project risk report generated: reportId={}, projectId={}, userId={}, level={}, score={}",
                report.getId(), projectId, userId, report.getRiskLevel(), report.getRiskScore());
        return toVO(report, currentUserName());
    }

    public List<ProjectRiskReportVO> list(Long projectId) {
        projectPermissionService.requireAccess(projectId);
        List<ProjectRiskReport> reports = riskReportMapper.selectList(
                new LambdaQueryWrapper<ProjectRiskReport>()
                        .eq(ProjectRiskReport::getProjectId, projectId)
                        .orderByDesc(ProjectRiskReport::getCreatedAt)
        );
        Map<Long, User> users = reports.isEmpty() ? Map.of() : userMapper.selectByIds(
                        reports.stream().map(ProjectRiskReport::getCreatorId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return reports.stream().map(report -> toVO(report, displayName(users.get(report.getCreatorId())))).toList();
    }

    public ProjectRiskReportVO detail(Long reportId) {
        ProjectRiskReport report = requireAccessible(reportId);
        return toVO(report, displayName(userMapper.selectById(report.getCreatorId())));
    }

    @Transactional
    public ProjectRiskReportVO update(Long reportId, ProjectRiskReportUpdateDTO request) {
        ProjectRiskReport report = requireEditable(reportId);
        report.setTitle(request.title().trim());
        report.setContent(request.content().trim());
        report.setUpdatedAt(LocalDateTime.now());
        riskReportMapper.updateById(report);
        log.info("AI project risk report updated: reportId={}, userId={}", reportId, UserContext.getUserId());
        return toVO(report, displayName(userMapper.selectById(report.getCreatorId())));
    }

    @Transactional
    public void delete(Long reportId) {
        ProjectRiskReport report = requireEditable(reportId);
        riskReportMapper.deleteById(reportId);
        log.info("AI project risk report deleted: reportId={}, projectId={}, userId={}",
                reportId, report.getProjectId(), UserContext.getUserId());
    }

    private RiskContext buildContext(Project project, Long userId) {
        LocalDate today = LocalDate.now();
        List<Task> allTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getProjectId, project.getId())
                .orderByAsc(Task::getDueDate)
                .last("LIMIT 500"));
        List<Task> activeTasks = allTasks.stream().filter(this::isActive).toList();
        List<ProjectMember> members = projectMemberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, project.getId()));
        List<Long> userIds = members.stream().map(ProjectMember::getUserId).distinct().toList();
        Map<Long, User> users = userIds.isEmpty() ? Map.of() : userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        long overdue = activeTasks.stream().filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(today)).count();
        long dueSoon = activeTasks.stream().filter(task -> task.getDueDate() != null
                && !task.getDueDate().isBefore(today) && !task.getDueDate().isAfter(today.plusDays(7))).count();
        long unassigned = activeTasks.stream().filter(task -> task.getAssigneeId() == null).count();
        long urgent = activeTasks.stream().filter(task -> task.getPriority() == TaskPriority.URGENT).count();
        long lowProgressNearDeadline = activeTasks.stream().filter(task -> task.getDueDate() != null
                && !task.getDueDate().isAfter(today.plusDays(14)) && safeProgress(task) < 50).count();

        Map<Long, Long> workload = activeTasks.stream()
                .filter(task -> task.getAssigneeId() != null)
                .collect(Collectors.groupingBy(Task::getAssigneeId, Collectors.counting()));
        long overloadedMembers = workload.values().stream().filter(count -> count >= 5).count();
        int progress = projectProgressService.calculate(allTasks);
        Long daysRemaining = project.getEndDate() == null ? null : ChronoUnit.DAYS.between(today, project.getEndDate());

        Map<String, Integer> scoreBreakdown = new LinkedHashMap<>();
        scoreBreakdown.put("overdueTasks", Math.min(40, Math.toIntExact(overdue * 12)));
        scoreBreakdown.put("urgentTasks", Math.min(15, Math.toIntExact(urgent * 5)));
        scoreBreakdown.put("dueSoonTasks", Math.min(15, Math.toIntExact(dueSoon * 4)));
        scoreBreakdown.put("unassignedTasks", Math.min(10, Math.toIntExact(unassigned * 3)));
        scoreBreakdown.put("memberWorkload", Math.min(10, Math.toIntExact(overloadedMembers * 5)));
        int scheduleScore = daysRemaining != null && daysRemaining < 0 && progress < 100
                ? 20 : daysRemaining != null && daysRemaining <= 14 && progress < 80 ? 10 : 0;
        scoreBreakdown.put("projectSchedule", scheduleScore);
        int score = Math.min(100, scoreBreakdown.values().stream().mapToInt(Integer::intValue).sum());
        ProjectRiskLevel level = riskLevel(score);

        List<Map<String, Object>> taskData = activeTasks.stream().map(task -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", task.getId());
            item.put("title", task.getTitle());
            item.put("description", task.getDescription());
            item.put("status", task.getStatus());
            item.put("priority", task.getPriority());
            item.put("progress", safeProgress(task));
            item.put("assignee", displayName(users.get(task.getAssigneeId())));
            item.put("startDate", task.getStartDate());
            item.put("dueDate", task.getDueDate());
            item.put("overdue", task.getDueDate() != null && task.getDueDate().isBefore(today));
            return item;
        }).toList();
        List<Map<String, Object>> workloadData = members.stream().map(member -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", member.getUserId());
            item.put("name", displayName(users.get(member.getUserId())));
            item.put("role", member.getRole());
            item.put("activeTasks", workload.getOrDefault(member.getUserId(), 0L));
            return item;
        }).toList();
        List<Map<String, Object>> documents = documentMapper.selectList(new LambdaQueryWrapper<Document>()
                        .eq(Document::getProjectId, project.getId())
                        .eq(Document::getVectorStatus, DocumentStatus.SUCCESS)
                        .orderByDesc(Document::getUpdatedAt)
                        .last("LIMIT 50"))
                .stream().map(document -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", document.getId());
                    item.put("name", document.getOriginalName());
                    item.put("type", document.getFileType());
                    item.put("updatedAt", document.getUpdatedAt());
                    return item;
                }).toList();

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("generatedAt", LocalDateTime.now());
        snapshot.put("totalTasks", allTasks.size());
        snapshot.put("activeTasks", activeTasks.size());
        snapshot.put("completedTasks", allTasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count());
        snapshot.put("overdueTasks", overdue);
        snapshot.put("dueSoonTasks", dueSoon);
        snapshot.put("urgentTasks", urgent);
        snapshot.put("unassignedTasks", unassigned);
        snapshot.put("lowProgressNearDeadlineTasks", lowProgressNearDeadline);
        snapshot.put("overloadedMembers", overloadedMembers);
        snapshot.put("projectProgress", progress);
        snapshot.put("daysRemaining", daysRemaining);
        snapshot.put("scoreBreakdown", scoreBreakdown);
        snapshot.put("workload", workloadData);

        Map<String, Object> projectData = new LinkedHashMap<>();
        projectData.put("id", project.getId());
        projectData.put("name", project.getName());
        projectData.put("description", project.getDescription());
        projectData.put("status", project.getStatus());
        projectData.put("progress", progress);
        projectData.put("startDate", project.getStartDate());
        projectData.put("endDate", project.getEndDate());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", project.getId());
        payload.put("userId", userId);
        payload.put("riskLevel", level);
        payload.put("riskScore", score);
        payload.put("project", projectData);
        payload.put("indicators", snapshot);
        payload.put("tasks", taskData);
        payload.put("members", workloadData);
        payload.put("documents", documents);
        return new RiskContext(payload, snapshot, score, level);
    }

    private ProjectRiskReport requireAccessible(Long reportId) {
        ProjectRiskReport report = riskReportMapper.selectById(reportId);
        if (report == null) throw new BusinessException(ErrorCode.PROJECT_RISK_REPORT_NOT_FOUND);
        projectPermissionService.requireAccess(report.getProjectId());
        return report;
    }

    private ProjectRiskReport requireEditable(Long reportId) {
        ProjectRiskReport report = requireAccessible(reportId);
        Long userId = UserContext.getUserId();
        Project project = projectPermissionService.getProjectOrThrow(report.getProjectId());
        if (!Objects.equals(report.getCreatorId(), userId) && !projectPermissionService.canManage(project, userId)) {
            throw new BusinessException(ErrorCode.PROJECT_RISK_REPORT_ACCESS_DENIED);
        }
        return report;
    }

    private boolean isActive(Task task) {
        return task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.CANCELLED;
    }

    private int safeProgress(Task task) {
        return task.getProgress() == null ? 0 : task.getProgress();
    }

    private ProjectRiskLevel riskLevel(int score) {
        if (score >= 75) return ProjectRiskLevel.CRITICAL;
        if (score >= 50) return ProjectRiskLevel.HIGH;
        if (score >= 25) return ProjectRiskLevel.MEDIUM;
        return ProjectRiskLevel.LOW;
    }

    private String normalizeTitle(String title, String projectName) {
        return title == null || title.isBlank() ? projectName + "项目风险分析（" + LocalDate.now() + "）" : title.trim();
    }

    private String currentUserName() {
        return displayName(userMapper.selectById(UserContext.getUserId()));
    }

    private String displayName(User user) {
        if (user == null) return null;
        return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
    }

    private String writeJson(Object value, String label) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize {}", label, e);
            return null;
        }
    }

    private Map<String, Object> readSnapshot(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            log.warn("Could not deserialize risk analysis snapshot", e);
            return Map.of();
        }
    }

    private List<SemanticSearchResultVO> readSources(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            log.warn("Could not deserialize risk report sources", e);
            return List.of();
        }
    }

    private ProjectRiskReportVO toVO(ProjectRiskReport report, String creatorName) {
        ProjectRiskReportVO vo = new ProjectRiskReportVO();
        vo.setId(report.getId());
        vo.setProjectId(report.getProjectId());
        vo.setCreatorId(report.getCreatorId());
        vo.setCreatorName(creatorName);
        vo.setTitle(report.getTitle());
        vo.setRiskLevel(report.getRiskLevel());
        vo.setRiskScore(report.getRiskScore());
        vo.setContent(report.getContent());
        vo.setAnalysisSnapshot(readSnapshot(report.getAnalysisSnapshot()));
        vo.setSources(readSources(report.getSources()));
        vo.setModel(report.getModel());
        vo.setCreatedAt(report.getCreatedAt());
        vo.setUpdatedAt(report.getUpdatedAt());
        return vo;
    }

    private record RiskContext(
            Map<String, Object> payload,
            Map<String, Object> snapshot,
            int score,
            ProjectRiskLevel level
    ) { }
}
