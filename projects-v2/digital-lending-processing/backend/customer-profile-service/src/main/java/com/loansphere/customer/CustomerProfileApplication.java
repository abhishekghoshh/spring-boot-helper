package com.loansphere.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.loansphere.common", "com.loansphere.customer"})
@EnableDiscoveryClient
@EnableFeignClients
public class CustomerProfileApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerProfileApplication.class, args);
    }
}
