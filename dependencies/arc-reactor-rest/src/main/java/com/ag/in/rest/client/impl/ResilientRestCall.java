package com.ag.in.rest.client.impl;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.ag.in.rest.client.RestCall;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ag.in.rest.client.exception.RestCallException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ResilientRestCall implements RestCall {

//    @Autowired
    private RestTemplate restTemplate = new RestTemplate();


    @Autowired
    private CircuitBreakerFactory circuitBreakerFactory;

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    private static final Logger log = LoggerFactory.getLogger(ResilientRestCall.class);

    @Override
    public <T> ResponseEntity<T> fireRestCall(String hystrixKey, URI uri, HttpMethod httpMethod,
                                              HttpEntity httpEntity, Class<T> type) throws RestCallException {
        log.debug("hystrix-key {}", hystrixKey);
        CircuitBreaker circuitBreaker = circuitBreaker(hystrixKey);
        log.debug("uri is {}", uri);
        Map<String, String> context = context();
        log.debug("current thread context {}", context);
        log.debug("headers are {}", httpEntity.getHeaders());
        log.debug("request body {}", httpEntity.getBody());
        try {
            return circuitBreaker.run(() -> {
                MDC.setContextMap(context);
                return restTemplate.exchange(uri, httpMethod, httpEntity, type);
            }, throwable -> {
                log.debug("exception while calling is {}", throwable.getMessage());
                throw new RuntimeException(throwable);
            });
        } catch (RuntimeException runtimeException) {
            return buildException(runtimeException, uri, hystrixKey);
        }
    }

    @Override
    public <T> ResponseEntity<T> fireRestCall(String hystrixKey, URI uri,
                                              HttpMethod httpMethod, HttpEntity httpEntity, Class<T> type, T defaultValue) {
        try {
            return fireRestCall(hystrixKey, uri, httpMethod, httpEntity, type);
        } catch (Throwable throwable) {
            return ResponseEntity.ok(defaultValue);
        }
    }

    @Override
    public <T> ResponseEntity<T> fireRestCall(String hystrixKey, String serviceKey, String api,
                                              HttpMethod httpMethod, HttpEntity httpEntity, Class<T> type) throws RestCallException {
        return fireRestCall(hystrixKey,
                serviceKey, api, null, null,
                httpMethod, httpEntity, type);
    }


    @Override
    public <T> ResponseEntity<T> fireRestCall(String hystrixKey, String serviceKey, String api,
                                              HttpMethod httpMethod, HttpEntity httpEntity, Class<T> type, T defaultValue) {
        try {
            return fireRestCall(hystrixKey, serviceKey, api, httpMethod, httpEntity, type);
        } catch (Exception ex) {
            return ResponseEntity.ok(defaultValue);
        }
    }

    @Override
    public <T> ResponseEntity<T> fireRestCall(String hystrixKey, String serviceKey, String api, Map<String, String> pathVariables,
                                              MultiValueMap<String, String> queryParameters, HttpMethod httpMethod, HttpEntity httpEntity, Class<T> type) throws RestCallException {
        return fireRestCall(hystrixKey,
                uri(serviceKey, api, pathVariables, queryParameters),
                httpMethod, httpEntity, type);
    }

    @Override
    public <T> ResponseEntity<T> fireRestCall(String hystrixKey, String serviceKey, String api, Map<String, String> pathVariables,
                                              MultiValueMap<String, String> queryParameters, HttpMethod httpMethod, HttpEntity httpEntity, Class<T> type, T defaultValue) {
        try {
            return fireRestCall(hystrixKey,
                    serviceKey, api, pathVariables, queryParameters,
                    httpMethod, httpEntity, type);
        } catch (Exception ex) {
            return ResponseEntity.ok(defaultValue);
        }
    }

    private URI uri(String serviceKey, String api, Map<String, String> pathVariables,
                    MultiValueMap<String, String> queryParameters) {
        String url = serviceKey.startsWith("http") ? serviceKey : loadBalancerClient.choose(serviceKey).getUri().toString();
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(url + api);
        if (!CollectionUtils.isEmpty(pathVariables)) {
            Map<String, Object> uriVariables = new HashMap<>();
            pathVariables.entrySet().forEach(entry -> uriVariables.put(entry.getKey(), entry.getValue()));
            uriComponentsBuilder.uriVariables(uriVariables);
        }
        if (!CollectionUtils.isEmpty(queryParameters)) {
            uriComponentsBuilder.queryParams(queryParameters);
        }
        return uriComponentsBuilder.build().toUri();
    }

    private <T> ResponseEntity<T> buildException(Throwable throwable, URI uri, String hystrixKey) throws RestCallException {
        Throwable cause = throwable.getCause();
        if (cause instanceof HttpClientErrorException) {
            HttpClientErrorException httpClientErrorException = (HttpClientErrorException) cause;
            throw new RestCallException("Unauthorized access",
                    httpClientErrorException.getStatusCode(),
                    httpClientErrorException.getResponseBodyAsString());
        } else if (cause instanceof TimeoutException) {
            TimeoutException timeoutException = (TimeoutException) cause;
            throw new RestCallException("Backend server hystrix timeout for hystrix-key : " + hystrixKey,
                    timeoutException.getMessage());
        } else if (cause instanceof ResourceAccessException && cause.getCause() instanceof SocketException) {
            SocketException socketException = (SocketException) cause.getCause();
            throw new RestCallException("Backend server unavailable",
                    socketException.getMessage() + " , Unable to access " + uri.getHost() + " , for hystrix-key " + hystrixKey);
        } else if (cause instanceof ResourceAccessException && cause.getCause() instanceof SocketTimeoutException) {
            SocketTimeoutException socketTimeoutException = (SocketTimeoutException) cause.getCause();
            throw new RestCallException("Backend server socket timeout exception",
                    socketTimeoutException.getMessage() + " , for the url " + uri.getHost() + " , for hystrix-key " + hystrixKey);
        }
        throw new RestCallException("Internal server error", cause.getMessage());
    }

    private Map<String, String> context() {
        if (null != MDC.getCopyOfContextMap()) return MDC.getCopyOfContextMap();
        if (null != ThreadContext.getContext()) return ThreadContext.getContext();
        return new HashMap<>();
    }

    private CircuitBreaker circuitBreaker(String hystrixKey) {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create(hystrixKey);
        if (null == circuitBreaker) {
            circuitBreaker = circuitBreakerFactory.create("default");
        }
        return circuitBreaker;
    }

}
