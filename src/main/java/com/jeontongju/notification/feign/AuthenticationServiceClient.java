package com.jeontongju.notification.feign;

import com.jeontongju.notification.dto.temp.MemberEmailForKeyDto;
import io.github.bitbox.bitbox.dto.FeignFormat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "authentication-service", url = "${endpoint.authentication-service}")
public interface AuthenticationServiceClient {

  @PostMapping("/members/email")
  FeignFormat<MemberEmailForKeyDto> getMemberEmailForKey(Long memberId);
}
