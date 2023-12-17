package com.jeontongju.notification.controller;

import com.jeontongju.notification.service.NotificationService;
import io.github.bitbox.bitbox.enums.MemberRoleEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;

    @GetMapping("/notifications/connect")
    public SseEmitter connect(@RequestHeader Long memberId, @RequestHeader MemberRoleEnum memberRole, @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        return notificationService.subscribe(memberId, memberRole, lastEventId);
    }
}
