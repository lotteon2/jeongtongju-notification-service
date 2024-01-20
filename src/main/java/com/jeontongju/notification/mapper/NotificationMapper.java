package com.jeontongju.notification.mapper;

import com.jeontongju.notification.domain.Notification;
import com.jeontongju.notification.dto.response.NotificationInfoForInquiryResponseDto;
import com.jeontongju.notification.dto.response.NotificationInfoForSingleInquiryDto;
import com.jeontongju.notification.dto.response.NotificationInfoResponseDto;
import com.jeontongju.notification.dto.response.UrlForRedirectResponseDto;
import io.github.bitbox.bitbox.enums.NotificationTypeEnum;
import io.github.bitbox.bitbox.enums.RecipientTypeEnum;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

  public Notification toEntity(
      Long recipientId,
      RecipientTypeEnum recipientTypeEnum,
      NotificationTypeEnum notificationTypeEnum) {

    String redirectLink;
    if (notificationTypeEnum == NotificationTypeEnum.OUT_OF_STOCK) {
      redirectLink = "https://seller.jeontongju.shop/product/list";
    } else if (notificationTypeEnum == NotificationTypeEnum.BALANCE_ACCOUNTS) {
      redirectLink = "https://seller.jeontongju.shop/cash/up";
    } else if (notificationTypeEnum == NotificationTypeEnum.SUCCESS_SUBSCRIPTION_PAYMENTS) {
      redirectLink = "https://jeontongju.shop/membership/list";
    } else {
      redirectLink = "https://jeontongju.shop/orderdetail";
    }
    return Notification.builder()
        .recipientId(recipientId)
        .recipientTypeEnum(recipientTypeEnum)
        .notificationTypeEnum(notificationTypeEnum)
        .redirectLink(redirectLink)
        .build();
  }

  public Notification toIncludedRedirectLinkEntity(
      Long recipientId,
      RecipientTypeEnum recipientTypeEnum,
      NotificationTypeEnum notificationType,
      String redirectLink) {

    return Notification.builder()
        .recipientId(recipientId)
        .recipientTypeEnum(recipientTypeEnum)
        .notificationTypeEnum(notificationType)
        .redirectLink(redirectLink)
        .build();
  }

  public List<NotificationInfoForSingleInquiryDto> toListLookupDto(
      List<Notification> notifications) {

    List<NotificationInfoForSingleInquiryDto> inquiryResponseDtos = new ArrayList<>();
    for (Notification notification : notifications) {

      NotificationInfoForSingleInquiryDto build =
          NotificationInfoForSingleInquiryDto.builder()
              .notificationId(notification.getNotificationId())
              .notificationType(notification.getNotificationTypeEnum())
              .isRead(notification.getIsRead())
              .createdAt(notification.getCreatedAt())
              .build();
      inquiryResponseDtos.add(build);
    }
    return inquiryResponseDtos;
  }

  public NotificationInfoForInquiryResponseDto toInquiryDto(
      int notReadCounts, List<NotificationInfoForSingleInquiryDto> notifications) {

    return NotificationInfoForInquiryResponseDto.builder()
        .notReadcounts(notReadCounts)
        .notifications(notifications)
        .build();
  }

  public NotificationInfoResponseDto toNotificationDto(Long notificationId, String redirectUrl, Object data) {

    if(data instanceof Notification && redirectUrl == null) {
      Notification savedNotification = (Notification) data;

      NotificationTypeEnum notificationTypeEnum = savedNotification.getNotificationTypeEnum();
      if (notificationTypeEnum == NotificationTypeEnum.OUT_OF_STOCK) { // 재고 소진
        redirectUrl = "https://jeontongju-front-jumo-nine.vercel.app/product/list";
      } else if (notificationTypeEnum == NotificationTypeEnum.BALANCE_ACCOUNTS) { // 정산
        redirectUrl = "https://jeontongju-front-jumo-nine.vercel.app/cash/up";
      } else if (notificationTypeEnum == NotificationTypeEnum.SUCCESS_SUBSCRIPTION_PAYMENTS) { // 구독 결제
        redirectUrl = "https://jeontongju.shop/membership/list";
      }
    }
    return NotificationInfoResponseDto.builder()
            .notificationId(notificationId)
            .redirectUrl(redirectUrl)
            .data(data)
            .build();
  }

  public UrlForRedirectResponseDto toRedirectUrlDto(String redirectUrl) {

    return UrlForRedirectResponseDto.builder().redirectUrl(redirectUrl).build();
  }
}
