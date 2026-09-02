package com.researchflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.context.UserContext;
import com.researchflow.entity.Notification;
import com.researchflow.entity.Task;
import com.researchflow.enums.NotificationType;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.NotificationMapper;
import com.researchflow.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public List<NotificationVO> listCurrentUser() {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, UserContext.getUserId())
                        .orderByAsc(Notification::getReadAt)
                        .orderByDesc(Notification::getCreatedAt)
                        .last("LIMIT 100"))
                .stream().map(this::toVO).toList();
    }

    public long unreadCount() {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, UserContext.getUserId())
                .isNull(Notification::getReadAt));
    }

    @Transactional
    public NotificationVO markRead(Long notificationId) {
        Notification notification = requireOwned(notificationId);
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
        return toVO(notification);
    }

    @Transactional
    public void markAllRead() {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, UserContext.getUserId())
                .isNull(Notification::getReadAt)
                .set(Notification::getReadAt, LocalDateTime.now()));
    }

    @Transactional
    public void taskAssigned(Task task, Long operatorId) {
        if (task.getAssigneeId() == null || Objects.equals(task.getAssigneeId(), operatorId)) return;
        create(task.getAssigneeId(), task.getProjectId(), NotificationType.TASK_ASSIGNED,
                "你有一项新的研究任务", "“" + task.getTitle() + "”已指派给你。",
                "TASK", task.getId(), "/tasks?taskId=" + task.getId(), null);
    }

    @Transactional
    public void taskChanged(
            Task task,
            Long operatorId,
            Collection<Long> recipientIds,
            NotificationType type,
            String title,
            String content
    ) {
        Set<Long> recipients = new LinkedHashSet<>(recipientIds);
        recipients.remove(null);
        boolean operatorIsRelated = operatorId != null && recipients.remove(operatorId);
        // Prefer notifying the other related users. When the operator is the only
        // related user, retain a self notification so the task event is not lost.
        if (recipients.isEmpty() && operatorIsRelated) {
            recipients.add(operatorId);
        }
        for (Long userId : recipients) {
            create(userId, task.getProjectId(), type, title, content,
                    "TASK", task.getId(), "/tasks?taskId=" + task.getId(), null);
        }
    }

    @Transactional
    public void aiResultReady(
            Long userId,
            Long projectId,
            NotificationType type,
            String title,
            String content,
            String targetType,
            Long targetId,
            String targetPath
    ) {
        create(userId, projectId, type, title, content, targetType, targetId, targetPath,
                type + ":" + targetId);
    }

    @Transactional
    public void createOnce(
            Long userId,
            Long projectId,
            NotificationType type,
            String title,
            String content,
            String targetType,
            Long targetId,
            String targetPath,
            String businessKey
    ) {
        create(userId, projectId, type, title, content, targetType, targetId, targetPath, businessKey);
    }

    private void create(
            Long userId,
            Long projectId,
            NotificationType type,
            String title,
            String content,
            String targetType,
            Long targetId,
            String targetPath,
            String businessKey
    ) {
        if (userId == null) return;
        if (businessKey != null && notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getBusinessKey, businessKey)) > 0) return;
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setProjectId(projectId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setTargetPath(targetPath);
        notification.setBusinessKey(businessKey);
        notification.setCreatedAt(LocalDateTime.now());
        try {
            notificationMapper.insert(notification);
        } catch (DuplicateKeyException ignored) {
            log.debug("Duplicate notification ignored: userId={}, businessKey={}", userId, businessKey);
        }
    }

    private Notification requireOwned(Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !Objects.equals(notification.getUserId(), UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        return notification;
    }

    private NotificationVO toVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setProjectId(notification.getProjectId());
        vo.setType(notification.getType());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setTargetType(notification.getTargetType());
        vo.setTargetId(notification.getTargetId());
        vo.setTargetPath(notification.getTargetPath());
        vo.setRead(notification.getReadAt() != null);
        vo.setReadAt(notification.getReadAt());
        vo.setCreatedAt(notification.getCreatedAt());
        return vo;
    }
}
