package com.loansphere.emi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.loansphere.common", "com.loansphere.emi"})
@EnableDiscoveryClient
public class EmiCalculationApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmiCalculationApplication.class, args);
    }
}
