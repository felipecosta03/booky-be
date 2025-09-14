package com.uade.bookybe.core.usecase;

public interface LivekitTokenService {

  String createJoinToken(String room, String identity, boolean canPublish, boolean canSubscribe);
}
