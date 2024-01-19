package com.jeontongju.notification.controller;

import com.jeontongju.notification.dto.request.FCMNotificationRequestDto;
import com.jeontongju.notification.service.FCMNotificationService;
import io.github.bitbox.bitbox.dto.ResponseFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FCMNotificationRestController {

  private final FCMNotificationService fcmNotificationService;

  @PostMapping("/notifications/fcm")
  public ResponseEntity<ResponseFormat<String>> sendNotificationByToken(
      @RequestHeader Long memberId, @RequestBody FCMNotificationRequestDto fcmNotificationDto) {

    fcmNotificationService.sendNotificationByToken(memberId, fcmNotificationDto);
    return ResponseEntity.ok()
        .body(
            ResponseFormat.<String>builder()
                .code(HttpStatus.OK.value())
                .message(HttpStatus.OK.name())
                .data(null)
                .build());
  }
}
