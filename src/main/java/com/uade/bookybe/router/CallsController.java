package com.uade.bookybe.router;

import com.uade.bookybe.config.LivekitProps;
import com.uade.bookybe.core.usecase.LivekitTokenService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/calls")
public class CallsController {

  private final LivekitTokenService tokenService;
  private final LivekitProps props;

  @PostMapping("/{room}/join")
  public Map<String, Object> join(@PathVariable String room, Authentication authentication) {
    String userId = authentication.getName();

    String token = tokenService.createJoinToken(room, userId, true, true);
    return Map.of("token", token, "url", props.getWsUrl());
  }
}
