package com.uade.bookybe;

import static com.uade.bookybe.util.EnvironmentUtil.setScope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BookyBeApplication {

  public static void main(String[] args) {
    setScope();
    SpringApplication.run(BookyBeApplication.class, args);
  }
}
