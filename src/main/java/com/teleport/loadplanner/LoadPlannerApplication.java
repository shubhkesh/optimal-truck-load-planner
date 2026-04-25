package com.teleport.loadplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class LoadPlannerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LoadPlannerApplication.class, args);
    }
}
