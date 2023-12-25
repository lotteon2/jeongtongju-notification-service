package com.jeontongju.notification.kafka;

import com.jeontongju.notification.dto.temp.MemberInfoForNotificationDto;
import com.jeontongju.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

  private final NotificationService notificationService;

  @KafkaListener(topics = "send-notification")
  public void sendNotification(MemberInfoForNotificationDto notificationDto) {

    log.info("NotificationConsumer's sendNotification");
    try {
      notificationService.send(
          notificationDto.getRecipient_id(),
          notificationDto.getRecipientType(),
          notificationDto.getNotificationType());
    } catch (Exception e) {
      log.error("During Send Event: Error while do notification={}", e.getMessage());
    }
  }
}
