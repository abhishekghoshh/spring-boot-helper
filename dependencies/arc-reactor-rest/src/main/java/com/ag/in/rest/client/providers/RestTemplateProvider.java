package com.ag.in.rest.client.providers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Configuration
public class RestTemplateProvider {

    @Bean
    public RestTemplate restTemplate(@Autowired Environment environment, @Autowired ApplicationContext applicationContext) {
        RestTemplate restTemplate = new RestTemplate();
        addInterceptor(restTemplate, environment, applicationContext);
        return restTemplate;
    }

    private void addInterceptor(RestTemplate restTemplate, Environment environment, ApplicationContext applicationContext) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (isRestInterceptorProfileExist(activeProfiles)) {
            ClientHttpRequestInterceptor clientHttpRequestInterceptor = applicationContext.getBean("restInterceptor", ClientHttpRequestInterceptor.class);
            restTemplate.setInterceptors(List.of(clientHttpRequestInterceptor));
        }
    }

    private static boolean isRestInterceptorProfileExist(String[] activeProfiles) {
        return null != activeProfiles
                && 0 != activeProfiles.length
                && Arrays.stream(activeProfiles)
                .filter(env -> "rest-interceptor".equalsIgnoreCase(env))
                .findFirst()
                .isPresent();
    }
}
