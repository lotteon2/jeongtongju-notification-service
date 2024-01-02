package com.jeontongju.notification.dto.temp;

import io.github.bitbox.bitbox.enums.NotificationTypeEnum;
import io.github.bitbox.bitbox.enums.RecipientTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class MemberInfoForNotificationDto {

  private Long recipient_id;
  private RecipientTypeEnum recipientType;
  private NotificationTypeEnum notificationType;
}
