package ru.unirate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UnirateApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnirateApplication.class, args);
    }
}
