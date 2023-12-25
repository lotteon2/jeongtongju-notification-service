package com.jeontongju.notification.dto.response;


import io.github.bitbox.bitbox.enums.NotificationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class NotificationInfoForSingleInquiryDto {

  private Long notificationId;
  private NotificationTypeEnum notificationType;
  private Boolean isRead;
  private LocalDateTime createdAt;
}
