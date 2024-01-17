package com.jeontongju.notification.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class UrlEncoderManager {
  public String encodeURIComponent(String value) {
    try {
      // URLEncoder.encode() 결과에서 '+'를 '%20'으로 변경
      return URLEncoder.encode(value, "UTF-8")
          .replaceAll("\\+", "%20")
          .replaceAll("\\%21", "!")
          .replaceAll("\\%27", "'")
          .replaceAll("\\%28", "(")
          .replaceAll("\\%29", ")")
          .replaceAll("\\%7E", "~");
    } catch (UnsupportedEncodingException e) {
      // 예외 처리 필요
      log.error("Error During encoding={}", e.getMessage());
      throw new RuntimeException();
    }
  }
}
