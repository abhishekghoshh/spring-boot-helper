package com.ag.in.rest.client;

import com.ag.in.rest.client.exception.RestCallException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public interface RestCall {

    public <T> ResponseEntity<T> fireRestCall(String hystrixKey, URI uri, HttpMethod httpMethod,
                                       HttpEntity httpEntity, Class<T> type) throws RestCallException;

    public <T> ResponseEntity<T> fireRestCall(String hystrixKey, URI uri, HttpMethod httpMethod,
                                       HttpEntity httpEntity, Class<T> type, T defaultValue);

    public <T> ResponseEntity<T> fireRestCall(String hystrixKey, String serviceKey, String api, HttpMethod httpMethod,
                                       HttpEntity httpEntity, Class<T> type) throws RestCallException;

    public <T> ResponseEntity<T> fireRestCall(String hystrixKey, String serviceKey, String api, HttpMethod httpMethod,
                                       HttpEntity httpEntity, Class<T> type, T defaultValue);

    public <T> ResponseEntity<T> fireRestCall(String hystrixKey, String serviceKey, String api, Map<String, String> pathVariables,
                                       MultiValueMap<String, String> queryParameters, HttpMethod httpMethod, HttpEntity httpEntity, Class<T> type) throws RestCallException;

    public <T> ResponseEntity<T> fireRestCall(String hystrixKey, String serviceKey, String api, Map<String, String> pathVariables,
                                       MultiValueMap<String, String> queryParameters, HttpMethod httpMethod, HttpEntity httpEntity, Class<T> type, T defaultValue);

}
