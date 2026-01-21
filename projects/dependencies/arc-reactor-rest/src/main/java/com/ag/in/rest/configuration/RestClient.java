package com.ag.in.rest.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({RestClientConfiguration.class})
@Configuration
public @interface RestClient {
}


