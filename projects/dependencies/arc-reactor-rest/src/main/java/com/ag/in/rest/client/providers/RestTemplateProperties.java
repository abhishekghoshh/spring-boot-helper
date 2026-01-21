package com.ag.in.rest.client.providers;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "rest-template.properties")
@Configuration
@Data
@NoArgsConstructor
public class RestTemplateProperties {
    private int connectionRequestTimeout = 1000;
    private int connectTimeout = 1000;
    private int readTimeout = 1000;
}
