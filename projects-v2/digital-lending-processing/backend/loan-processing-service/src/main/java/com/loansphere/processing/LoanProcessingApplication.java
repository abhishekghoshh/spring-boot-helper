package com.loansphere.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.loansphere.common", "com.loansphere.processing"})
@EnableDiscoveryClient
@EnableFeignClients
public class LoanProcessingApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanProcessingApplication.class, args);
    }
}
