package com.jeontongju.notification.feign;

import com.jeontongju.notification.dto.FCMTokenDto;
import io.github.bitbox.bitbox.dto.FeignFormat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "consumer-service")
public interface ConsumerServiceClient {

  @GetMapping("/consumers/{consumerId}/fcm-token")
  FeignFormat<FCMTokenDto> getConsumerFCMToken(@PathVariable Long consumerId);
}
