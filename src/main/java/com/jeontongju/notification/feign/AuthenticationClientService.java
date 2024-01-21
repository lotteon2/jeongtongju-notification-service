package com.jeontongju.notification.feign;

import com.jeontongju.notification.dto.temp.MemberEmailForKeyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthenticationClientService {

  private final AuthenticationServiceClient authenticationServiceClient;

  public MemberEmailForKeyDto getMemberEmailForKey(Long memberId) {
    return authenticationServiceClient.getMemberEmailForKey(memberId).getData();
  }

  public String getConsumerEmail(Long memberId) {

    return authenticationServiceClient.getMemberEmailForKey(memberId).getData().getEmail();
  }
}
