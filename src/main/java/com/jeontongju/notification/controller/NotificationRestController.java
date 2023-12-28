package com.jeontongju.notification.controller;

import com.jeontongju.notification.dto.response.NotificationInfoForInquiryResponseDto;
import com.jeontongju.notification.service.NotificationService;
import io.github.bitbox.bitbox.dto.ResponseFormat;
import io.github.bitbox.bitbox.enums.MemberRoleEnum;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
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
      @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "")
          String lastEventId) {

    return notificationService.subscribe(memberId, lastEventId);
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
  public ResponseEntity<ResponseFormat<Void>> readNotification(
      @PathVariable("notificationId") Long notificationId) {

    notificationService.readNotification(notificationId);
    return ResponseEntity.ok()
        .body(
            ResponseFormat.<Void>builder()
                .code(HttpStatus.OK.value())
                .message(HttpStatus.OK.name())
                .detail("해당 알림 읽음 처리 성공")
                .build());
  }

  @PatchMapping("/notifications")
  public ResponseEntity<ResponseFormat<Void>> readAllNotification(@RequestHeader Long memberId) {

    notificationService.readAllNotification(memberId);
    return ResponseEntity.ok()
        .body(
            ResponseFormat.<Void>builder()
                .code(HttpStatus.OK.value())
                .message(HttpStatus.OK.name())
                .detail("전체 읽음 처리 성공")
                .build());
  }

  @GetMapping("/notifications/{notificationId}/to")
  public void redirectByNotificationLink(
      @RequestHeader Long memberId, @PathVariable Long notificationId, HttpServletResponse response)
      throws IOException {

    String foundRedirectLink = notificationService.getRedirectLink(memberId, notificationId);
    if (foundRedirectLink != null) {
      response.sendRedirect(foundRedirectLink);
    }
  }

  @PostMapping("/notifications/test")
  public void test(@RequestHeader Long memberId, @RequestHeader MemberRoleEnum memberRole) {
    notificationService.testNotificationProduce(memberId, memberRole);
  }
}
