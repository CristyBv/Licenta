package com.licence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.licence")
public class LicenceProjectApplication {
    public static void main(String[] args) {
        SpringApplication.run(LicenceProjectApplication.class, args);
    }
}
