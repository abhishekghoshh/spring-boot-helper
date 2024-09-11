package com.ag.test;

import com.ag.in.rest.client.RestCall;
import com.ag.in.rest.configuration.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestClient
public class TestApplication implements CommandLineRunner {

    @Autowired
    RestCall restCall;


    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String hystrixKey = "default";
        String serviceKey = "https://reqres.in/";
        String api = "/api/users/{id}";
        Map<String, String> pathVariables = new HashMap<>();
        pathVariables.put("id","2");
        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        queryParameters.add("page", "2");
        queryParameters.add("page", "3");
        HttpMethod httpMethod = HttpMethod.GET;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(List.of(MediaType.ALL));
        HttpEntity httpEntity = new HttpEntity<>(httpHeaders);
        Map<String, String> defaultValue = new HashMap<String, String>();
        ResponseEntity<Map> responseEntity = restCall.fireRestCall(hystrixKey, serviceKey, api, pathVariables, null, httpMethod, httpEntity, Map.class, defaultValue);
        System.out.println(responseEntity.getBody());
        System.out.println(responseEntity.getHeaders());
    }
}
