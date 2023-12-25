package com.jeontongju.notification.mapper;

import com.jeontongju.notification.domain.Notification;

import io.github.bitbox.bitbox.enums.NotificationTypeEnum;
import io.github.bitbox.bitbox.enums.RecipientTypeEnum;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

  public Notification toEntity(
      Long recipientId,
      RecipientTypeEnum recipientTypeEnum,
      NotificationTypeEnum notificationTypeEnum) {

    return Notification.builder()
        .recipientId(recipientId)
        .recipientTypeEnum(recipientTypeEnum)
        .notificationTypeEnum(notificationTypeEnum)
        .build();
  }
}
