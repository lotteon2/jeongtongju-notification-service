package com.jeontongju.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.jeontongju.notification.dto.FCMTokenDto;
import com.jeontongju.notification.dto.request.FCMNotificationRequestDto;
import com.jeontongju.notification.feign.ConsumerClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FCMNotificationService {

  private final FirebaseMessaging firebaseMessaging;
  private final ConsumerClientService consumerClientService;

  public String sendNotificationByToken(
      Long memberId, FCMNotificationRequestDto fcmNotificationDto) {

    FCMTokenDto fcmTokenDto = consumerClientService.getConsumerFCMToken(memberId);

    Notification notification =
        Notification.builder()
            .setTitle(fcmNotificationDto.getTitle())
            .setBody(fcmNotificationDto.getBody())
            .build();

    Message message =
        Message.builder().setToken(fcmTokenDto.getFcmToken()).setNotification(notification).build();

    try {
      firebaseMessaging.send(message);
      return "Successful Send Notification. targetMemberId=" + memberId;
    } catch (FirebaseMessagingException e) {
      log.error("[During fcm Logic]: Error firebaseMessaging.send()={}", e.getMessage());
      return "Fail Sending Notification. targetMemberId=" + memberId;
    }
  }
}
