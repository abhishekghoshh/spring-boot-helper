package com.ag.in.rest.client.providers;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

@Configuration
public class HostNameVerificationProvider {

    public static final String TRUE = "true";

    @Bean
    public HostnameVerifier hostnameVerifier(@Value("${rest-template.strategy.verifyHostName:true}") String verifyHostName) {
        return TRUE.equalsIgnoreCase(verifyHostName) ?
                new DefaultHostnameVerifier() :
                (String hostname, SSLSession sslSession) -> true;
    }
}
