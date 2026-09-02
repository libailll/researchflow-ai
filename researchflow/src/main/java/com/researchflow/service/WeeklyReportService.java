package com.researchflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.context.UserContext;
import com.researchflow.dto.WeeklyReportGenerateDTO;
import com.researchflow.dto.WeeklyReportUpdateDTO;
import com.researchflow.entity.AgentActionAudit;
import com.researchflow.entity.Document;
import com.researchflow.entity.Project;
import com.researchflow.entity.Task;
import com.researchflow.entity.User;
import com.researchflow.entity.WeeklyReport;
import com.researchflow.enums.DocumentStatus;
import com.researchflow.enums.TaskStatus;
import com.researchflow.enums.NotificationType;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.AgentActionAuditMapper;
import com.researchflow.mapper.DocumentMapper;
import com.researchflow.mapper.TaskMapper;
import com.researchflow.mapper.UserMapper;
import com.researchflow.mapper.WeeklyReportMapper;
import com.researchflow.vo.SemanticSearchResultVO;
import com.researchflow.vo.WeeklyReportAiResultVO;
import com.researchflow.vo.WeeklyReportVO;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final WeeklyReportMapper weeklyReportMapper;
    private final TaskMapper taskMapper;
    private final DocumentMapper documentMapper;
    private final AgentActionAuditMapper actionAuditMapper;
    private final UserMapper userMapper;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectProgressService projectProgressService;
    private final AiServiceClient aiServiceClient;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public WeeklyReportVO generate(Long projectId, WeeklyReportGenerateDTO request) {
        Project project = projectPermissionService.requireAccess(projectId);
        validatePeriod(request.periodStart(), request.periodEnd());
        Long userId = UserContext.getUserId();
        Map<String, Object> context = buildContext(project, userId, request.periodStart(), request.periodEnd());
        WeeklyReportAiResultVO generated = aiServiceClient.generateWeeklyReport(context);
        if (generated.content() == null || generated.content().isBlank()) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        WeeklyReport report = new WeeklyReport();
        report.setProjectId(projectId);
        report.setCreatorId(userId);
        report.setTitle(normalizeTitle(request.title(), project.getName(), request.periodStart(), request.periodEnd()));
        report.setPeriodStart(request.periodStart());
        report.setPeriodEnd(request.periodEnd());
        report.setContent(generated.content().trim());
        report.setSources(writeSources(generated.sources()));
        report.setModel(generated.model());
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        weeklyReportMapper.insert(report);
        notificationService.aiResultReady(userId, projectId, NotificationType.WEEKLY_REPORT_READY,
                "项目周报已生成", "“" + report.getTitle() + "”已经生成并保存。",
                "WEEKLY_REPORT", report.getId(), "/weekly-reports?reportId=" + report.getId());
        log.info("AI weekly report generated: reportId={}, projectId={}, userId={}, period={}..{}",
                report.getId(), projectId, userId, request.periodStart(), request.periodEnd());
        return toVO(report, currentUserName());
    }

    public List<WeeklyReportVO> list(Long projectId) {
        projectPermissionService.requireAccess(projectId);
        List<WeeklyReport> reports = weeklyReportMapper.selectList(
                new LambdaQueryWrapper<WeeklyReport>()
                        .eq(WeeklyReport::getProjectId, projectId)
                        .orderByDesc(WeeklyReport::getPeriodEnd)
                        .orderByDesc(WeeklyReport::getCreatedAt)
        );
        Map<Long, User> users = reports.isEmpty() ? Map.of() : userMapper.selectByIds(
                        reports.stream().map(WeeklyReport::getCreatorId).distinct().toList()
                ).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return reports.stream()
                .map(report -> toVO(report, displayName(users.get(report.getCreatorId()))))
                .toList();
    }

    public WeeklyReportVO detail(Long reportId) {
        WeeklyReport report = requireAccessible(reportId);
        User creator = userMapper.selectById(report.getCreatorId());
        return toVO(report, displayName(creator));
    }

    @Transactional
    public WeeklyReportVO update(Long reportId, WeeklyReportUpdateDTO request) {
        WeeklyReport report = requireEditable(reportId);
        report.setTitle(request.title().trim());
        report.setContent(request.content().trim());
        report.setUpdatedAt(LocalDateTime.now());
        weeklyReportMapper.updateById(report);
        log.info("AI weekly report updated: reportId={}, userId={}", reportId, UserContext.getUserId());
        User creator = userMapper.selectById(report.getCreatorId());
        return toVO(report, displayName(creator));
    }

    @Transactional
    public void delete(Long reportId) {
        WeeklyReport report = requireEditable(reportId);
        weeklyReportMapper.deleteById(reportId);
        log.info("AI weekly report deleted: reportId={}, projectId={}, userId={}",
                reportId, report.getProjectId(), UserContext.getUserId());
    }

    private Map<String, Object> buildContext(
            Project project,
            Long userId,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        List<Task> allTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getProjectId, project.getId())
                .orderByAsc(Task::getDueDate)
                .last("LIMIT 300"));
        LocalDateTime start = periodStart.atStartOfDay();
        LocalDateTime endExclusive = periodEnd.plusDays(1).atStartOfDay();
        List<Task> periodTasks = allTasks.stream()
                .filter(task -> isInPeriod(task.getCreatedAt(), start, endExclusive)
                        || isInPeriod(task.getUpdatedAt(), start, endExclusive)
                        || isInPeriod(task.getCompletedAt(), start, endExclusive)
                        || isInPeriod(task.getDueDate(), periodStart, periodEnd)
                        || task.getStatus() == TaskStatus.IN_PROGRESS
                        || task.getStatus() == TaskStatus.REVIEW)
                .toList();
        List<Long> assigneeIds = periodTasks.stream()
                .map(Task::getAssigneeId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, User> assignees = assigneeIds.isEmpty() ? Map.of() : userMapper.selectByIds(assigneeIds)
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        long completed = allTasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        long overdue = allTasks.stream()
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now()))
                .filter(task -> task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.CANCELLED)
                .count();

        List<Map<String, Object>> tasks = periodTasks.stream().map(task -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", task.getId());
            item.put("title", task.getTitle());
            item.put("description", task.getDescription());
            item.put("status", task.getStatus());
            item.put("priority", task.getPriority());
            item.put("progress", task.getProgress());
            item.put("assignee", displayName(assignees.get(task.getAssigneeId())));
            item.put("startDate", task.getStartDate());
            item.put("dueDate", task.getDueDate());
            item.put("completedAt", task.getCompletedAt());
            item.put("updatedAt", task.getUpdatedAt());
            return item;
        }).toList();

        List<Map<String, Object>> documents = documentMapper.selectList(new LambdaQueryWrapper<Document>()
                        .eq(Document::getProjectId, project.getId())
                        .eq(Document::getParseStatus, DocumentStatus.SUCCESS)
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

        List<Map<String, Object>> activities = actionAuditMapper.selectList(
                        new LambdaQueryWrapper<AgentActionAudit>()
                                .eq(AgentActionAudit::getProjectId, project.getId())
                                .ge(AgentActionAudit::getCreatedAt, start)
                                .lt(AgentActionAudit::getCreatedAt, endExclusive)
                                .orderByDesc(AgentActionAudit::getCreatedAt)
                                .last("LIMIT 100"))
                .stream().map(audit -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("actionType", audit.getActionType());
                    item.put("targetType", audit.getTargetType());
                    item.put("targetId", audit.getTargetId());
                    item.put("status", audit.getStatus());
                    item.put("createdAt", audit.getCreatedAt());
                    return item;
                }).toList();

        Map<String, Object> projectData = new LinkedHashMap<>();
        projectData.put("id", project.getId());
        projectData.put("name", project.getName());
        projectData.put("description", project.getDescription());
        projectData.put("status", project.getStatus());
        projectData.put("progress", projectProgressService.calculate(allTasks));
        projectData.put("startDate", project.getStartDate());
        projectData.put("endDate", project.getEndDate());

        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("totalTasks", allTasks.size());
        statistics.put("completedTasks", completed);
        statistics.put("overdueTasks", overdue);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("projectId", project.getId());
        context.put("userId", userId);
        context.put("periodStart", periodStart);
        context.put("periodEnd", periodEnd);
        context.put("project", projectData);
        context.put("statistics", statistics);
        context.put("tasks", tasks);
        context.put("activities", activities);
        context.put("documents", documents);
        return context;
    }

    private WeeklyReport requireAccessible(Long reportId) {
        WeeklyReport report = weeklyReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.WEEKLY_REPORT_NOT_FOUND);
        }
        projectPermissionService.requireAccess(report.getProjectId());
        return report;
    }

    private WeeklyReport requireEditable(Long reportId) {
        WeeklyReport report = requireAccessible(reportId);
        Long userId = UserContext.getUserId();
        Project project = projectPermissionService.getProjectOrThrow(report.getProjectId());
        if (!report.getCreatorId().equals(userId) && !projectPermissionService.canManage(project, userId)) {
            throw new BusinessException(ErrorCode.WEEKLY_REPORT_ACCESS_DENIED);
        }
        return report;
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > 31) {
            throw new BusinessException(ErrorCode.WEEKLY_REPORT_PERIOD_INVALID);
        }
    }

    private String normalizeTitle(String title, String projectName, LocalDate start, LocalDate end) {
        return title == null || title.isBlank()
                ? projectName + "周报（" + start + " 至 " + end + "）"
                : title.trim();
    }

    private boolean isInPeriod(LocalDateTime value, LocalDateTime start, LocalDateTime endExclusive) {
        return value != null && !value.isBefore(start) && value.isBefore(endExclusive);
    }

    private boolean isInPeriod(LocalDate value, LocalDate start, LocalDate end) {
        return value != null && !value.isBefore(start) && !value.isAfter(end);
    }

    private String currentUserName() {
        return displayName(userMapper.selectById(UserContext.getUserId()));
    }

    private String displayName(User user) {
        if (user == null) return null;
        return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
    }

    private String writeSources(List<SemanticSearchResultVO> sources) {
        if (sources == null || sources.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize weekly report sources", e);
            return null;
        }
    }

    private List<SemanticSearchResultVO> readSources(String sources) {
        if (sources == null || sources.isBlank()) return List.of();
        try {
            return objectMapper.readValue(sources, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            log.warn("Could not deserialize weekly report sources", e);
            return List.of();
        }
    }

    private WeeklyReportVO toVO(WeeklyReport report, String creatorName) {
        WeeklyReportVO vo = new WeeklyReportVO();
        vo.setId(report.getId());
        vo.setProjectId(report.getProjectId());
        vo.setCreatorId(report.getCreatorId());
        vo.setCreatorName(creatorName);
        vo.setTitle(report.getTitle());
        vo.setPeriodStart(report.getPeriodStart());
        vo.setPeriodEnd(report.getPeriodEnd());
        vo.setContent(report.getContent());
        vo.setSources(readSources(report.getSources()));
        vo.setModel(report.getModel());
        vo.setCreatedAt(report.getCreatedAt());
        vo.setUpdatedAt(report.getUpdatedAt());
        return vo;
    }
}
