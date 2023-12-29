package com.springboottest.springboot_01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@EnableCaching
public class Springboot01Application {
    public static void main(String[] args){
        SpringApplication.run(Springboot01Application.class, args);
    }
}


