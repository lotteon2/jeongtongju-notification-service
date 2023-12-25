package com.jeontongju.notification.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class NotificationInfoForInquiryResponseDto {

  private int notReadcounts;
  private List<NotificationInfoForSingleInquiryDto> notifications;
}
