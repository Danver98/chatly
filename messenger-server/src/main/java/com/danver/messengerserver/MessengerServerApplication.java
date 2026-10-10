package com.danver.messengerserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication()
public class MessengerServerApplication {

    public static void main(String[] args) {
        // Preventing from ClassCastException during development
        System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(MessengerServerApplication.class, args);
    }
}
