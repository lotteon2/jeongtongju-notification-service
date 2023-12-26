package com.jeontongju.notification.kafka;

import com.jeontongju.notification.dto.temp.MemberInfoForNotificationDto;
import com.jeontongju.notification.service.NotificationService;
import io.github.bitbox.bitbox.dto.ServerErrorForNotificationDto;
import io.github.bitbox.bitbox.util.KafkaTopicNameInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

  private final NotificationService notificationService;

  @KafkaListener(topics = KafkaTopicNameInfo.SEND_NOTIFICATION)
  public void sendNotification(MemberInfoForNotificationDto notificationDto) {

    log.info("NotificationConsumer's sendNotification executes..");
    try {
      notificationService.send(
          notificationDto.getRecipient_id(),
          notificationDto.getRecipientType(),
          notificationDto.getNotificationType());
    } catch (Exception e) {
      log.error("During Send Event: Error while do notification={}", e.getMessage());
    }
  }

  @KafkaListener(topics = KafkaTopicNameInfo.SEND_ERROR_NOTIFICATION)
  public void sendServerErrorNotification(
      ServerErrorForNotificationDto serverErrorNotificationDto) {

    log.info("NotificationConsumer's sendServerErrorNotification executes..");
    try {
      notificationService.sendError(serverErrorNotificationDto);
    } catch (Exception e) {
      log.error("During Send Event: Error while do server-error-notification={}", e.getMessage());
    }
  }
}
