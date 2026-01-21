package com.ag.in.rest.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;


@Configuration
@ComponentScan(basePackages = {"com.ag.in.rest"})
public class RestClientConfiguration implements ImportAware {

    private static final Logger log = LoggerFactory.getLogger(RestClientConfiguration.class);

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> values = importMetadata.getAnnotationAttributes(RestClient.class.getName());
        if (log.isDebugEnabled()) {
            log.debug("rest-client metadata are {}", values);
        }
    }
}
