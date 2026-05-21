package com.mphasis.tse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TradeStreamMain {
    public static void main(String[] args) {
        SpringApplication.run(TradeStreamMain.class, args);
    }
}
