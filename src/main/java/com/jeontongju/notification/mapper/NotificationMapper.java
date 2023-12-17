package com.jeontongju.notification.mapper;

import com.jeontongju.notification.domain.Notification;
import com.jeontongju.notification.enums.NotificationTypeEnum;
import com.jeontongju.notification.enums.RecipientTypeEnum;
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
