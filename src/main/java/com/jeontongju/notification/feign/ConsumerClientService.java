package com.jeontongju.notification.feign;

import com.jeontongju.notification.dto.FCMTokenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConsumerClientService {

  private final ConsumerServiceClient consumerServiceClient;

  public FCMTokenDto getConsumerFCMToken(Long consumerId) {

    return consumerServiceClient.getConsumerFCMToken(consumerId).getData();
  }
}
