package com.jeontongju.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FCMNotificationRequestDto {

  private String title;
  private String body;
}
