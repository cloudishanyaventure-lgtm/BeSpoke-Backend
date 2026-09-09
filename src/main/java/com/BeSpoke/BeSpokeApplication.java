package com.BeSpoke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // InboxService polls contact@bespokedesign.in
public class BeSpokeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeSpokeApplication.class, args);
    }
}
