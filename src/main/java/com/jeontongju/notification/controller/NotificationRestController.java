package com.jeontongju.notification.controller;

import com.jeontongju.notification.dto.response.NotificationInfoForInquiryResponseDto;
import com.jeontongju.notification.service.NotificationService;
import io.github.bitbox.bitbox.dto.ResponseFormat;
import io.github.bitbox.bitbox.enums.MemberRoleEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationRestController {

  private final NotificationService notificationService;

  @GetMapping("/notifications/connect")
  public SseEmitter connect(
      @RequestHeader Long memberId,
      @RequestHeader MemberRoleEnum memberRole,
      @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "")
          String lastEventId) {

    return notificationService.subscribe(memberId, memberRole, lastEventId);
  }

  @GetMapping("/notifications")
  public ResponseEntity<ResponseFormat<NotificationInfoForInquiryResponseDto>>
      getNotificationInfosForInquiry(
          @RequestHeader Long memberId, @RequestHeader MemberRoleEnum memberRole) {

    return ResponseEntity.ok()
        .body(
            ResponseFormat.<NotificationInfoForInquiryResponseDto>builder()
                .code(HttpStatus.OK.value())
                .message(HttpStatus.OK.name())
                .detail("알림 조회 성공")
                .data(notificationService.getNotificationInfosForInquiry(memberId, memberRole))
                .build());
  }

  @PatchMapping("/notifications/{notificationId}")
  public ResponseEntity<ResponseFormat<Void>> readProcessing(
      @PathVariable("notificationId") Long notificationId) {

    notificationService.readProcessing(notificationId);
    return ResponseEntity.ok()
        .body(
            ResponseFormat.<Void>builder()
                .code(HttpStatus.OK.value())
                .message(HttpStatus.OK.name())
                .detail("해당 알림 읽음 처리 성공")
                .build());
  }
}
