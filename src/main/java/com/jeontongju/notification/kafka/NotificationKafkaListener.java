package com.jeontongju.notification.kafka;

import com.jeontongju.notification.service.NotificationService;
import io.github.bitbox.bitbox.dto.MemberInfoForNotificationDto;
import io.github.bitbox.bitbox.dto.ServerErrorForNotificationDto;
import io.github.bitbox.bitbox.util.KafkaTopicNameInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaListener {

  private final NotificationService notificationService;

  @KafkaListener(topics = KafkaTopicNameInfo.SEND_NOTIFICATION)
  public void sendNotification(MemberInfoForNotificationDto notificationDto)
      throws InterruptedException {

    Thread.sleep(10000);
    log.info("NotificationConsumer's sendNotification executes..");
    try {
      notificationService.send(
          notificationDto.getRecipientId(),
          notificationDto.getRecipientType(),
          notificationDto.getNotificationType());
    } catch (Exception e) {
      log.error("During Send Event: Error while do notification={}", e.getMessage());
    }
  }

  @KafkaListener(topics = KafkaTopicNameInfo.SEND_ERROR_NOTIFICATION)
  public void sendServerErrorNotification(ServerErrorForNotificationDto serverErrorDto)
      throws InterruptedException {

    Thread.sleep(10000);
    log.info("NotificationConsumer's sendServerErrorNotification executes..");
    try {
      notificationService.sendError(serverErrorDto);
    } catch (Exception e) {
      log.error("During Send Event: Error while do server-error-notification={}", e.getMessage());
    }
  }

  @KafkaListener(topics = KafkaTopicNameInfo.SEND_ERROR_CANCELING_ORDER_NOTIFICATION)
  public void sendServerErrorCancelingOrderNotification(
      MemberInfoForNotificationDto memberInfoDto) {

    try {
      notificationService.sendCancelingServerError(memberInfoDto);
    } catch (Exception e) {
      log.error(
          "During Send Event: Error while recovering Order By Order Cancel Fail={}",
          e.getMessage());
    }
  }
}
