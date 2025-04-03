package io.github.abhishekghosh.jenkinshelper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SplunkHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(SplunkHelperApplication.class, args);
    }

//    @Bean
//    public TomcatProtocolHandlerCustomizer<?> tomcatProtocolHandlerCustomizer() {
//        return protocolHandler -> {
//            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
//        };
//    }
}
