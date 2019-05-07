package com.gennlife.fs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class)
public class ArkFileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArkFileServiceApplication.class, args);
    }

}
