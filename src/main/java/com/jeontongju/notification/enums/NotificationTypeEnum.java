package com.jeontongju.notification.enums;

public enum NotificationTypeEnum {
  OUT_OF_STOCK("재고 소진"),
  BALANCE_ACCOUNTS("정산"),
  SUCCESS_SUBSCRIPTION_PAYMENTS("구독결제 성공");

  private String value;

  NotificationTypeEnum(String value) {
    this.value = value;
  }
}
