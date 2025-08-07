package com.uade.bookybe.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class DefaultAdvisor {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Exception> handleException(Exception e) {
    log.error("Unexpected error: ", e);
    return ResponseEntity.status(400).body(e);
  }
}
