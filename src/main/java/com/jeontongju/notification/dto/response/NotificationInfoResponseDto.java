package com.jeontongju.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class NotificationInfoResponseDto {

  private Long notificationId;
  private String redirectUrl;
  private Object data;
}
