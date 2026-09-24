package br.com.bilca.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BilcaEventApplication {

    public static void main(String[] args) {
        SpringApplication.run(BilcaEventApplication.class, args);
    }
}
