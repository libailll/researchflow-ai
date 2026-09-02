package com.researchflow.controller;

import com.researchflow.common.Result;
import com.researchflow.service.NotificationService;
import com.researchflow.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "通知中心", description = "当前用户的业务通知、未读数量与已读状态")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "查询当前用户最近 100 条通知")
    public Result<List<NotificationVO>> list() {
        return Result.success(notificationService.listCurrentUser());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "查询当前用户未读通知数量")
    public Result<Map<String, Long>> unreadCount() {
        return Result.success(Map.of("count", notificationService.unreadCount()));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "将一条通知标记为已读")
    public Result<NotificationVO> markRead(
            @Positive(message = "通知ID必须大于 0") @PathVariable Long notificationId
    ) {
        return Result.success(notificationService.markRead(notificationId));
    }

    @PutMapping("/read-all")
    @Operation(summary = "将当前用户全部通知标记为已读")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.success();
    }
}
