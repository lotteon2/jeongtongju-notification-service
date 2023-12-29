package com.jeontongju.notification.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jeontongju.notification.dto.response.EmitterInfoForSingleInquiryDto;
import com.jeontongju.notification.dto.response.NotificationInfoForInquiryResponseDto;
import com.jeontongju.notification.service.NotificationService;
import io.github.bitbox.bitbox.dto.ResponseFormat;
import io.github.bitbox.bitbox.dto.ServerErrorForNotificationDto;
import io.github.bitbox.bitbox.enums.MemberRoleEnum;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationRestController {

  private final NotificationService notificationService;

  @GetMapping(value = "/notifications/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter connect(
      @RequestHeader Long memberId,
      @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "")
          String lastEventId,
      HttpServletResponse response) {
    response.addHeader("X-Accel-Buffering", "no");
    response.addHeader(HttpHeaders.CONNECTION, "keep-alive");
    response.addHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
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

  @GetMapping("/notifications/test")
  public ResponseEntity<ResponseFormat<List<EmitterInfoForSingleInquiryDto>>> test2(
      @RequestHeader Long memberId, @RequestHeader MemberRoleEnum memberRole) {

    return ResponseEntity.ok()
        .body(
            ResponseFormat.<List<EmitterInfoForSingleInquiryDto>>builder()
                .code(HttpStatus.OK.value())
                .message(HttpStatus.OK.name())
                .detail("emitters 목록 조회 성공")
                .data(notificationService.getEmitters(memberId, memberRole))
                .build());
  }

  @PostMapping("/test/fail")
  public ResponseEntity<ResponseFormat<Void>> testFail(
      @RequestHeader Long memberId, @RequestBody ServerErrorForNotificationDto serverErrorDto)
      throws JsonProcessingException {

    notificationService.sendError(serverErrorDto);
    return ResponseEntity.ok()
        .body(
            ResponseFormat.<Void>builder()
                .code(HttpStatus.OK.value())
                .message(HttpStatus.OK.name())
                .detail("주문 실패 로직 성공")
                .build());
  }
}
