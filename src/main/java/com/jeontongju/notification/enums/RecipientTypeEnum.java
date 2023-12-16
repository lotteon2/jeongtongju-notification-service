package com.jeontongju.notification.enums;

public enum RecipientTypeEnum {
  ROLE_CONSUMER("ROLE_CONSUMER"),
  ROLE_SELLER("ROLE_SELLER");

  private String value;

  RecipientTypeEnum(String value) {
    this.value = value;
  }
}
