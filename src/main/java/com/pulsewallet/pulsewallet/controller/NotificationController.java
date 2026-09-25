package com.pulsewallet.pulsewallet.controller;

import java.util.List;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.NotificationResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(notificationService.list(requirePrincipal(principal).getId()));
    }

    @GetMapping("/unread")
    public ApiResponse<List<NotificationResponse>> listUnread(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(notificationService.listUnread(requirePrincipal(principal).getId()));
    }

    @GetMapping("/unread/count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(notificationService.unreadCount(requirePrincipal(principal).getId()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Notification marked as read",
                notificationService.markRead(id, requirePrincipal(principal).getId()));
    }

    private UserPrincipal requirePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return principal;
    }
}
