package com.github.typicalitguy.chunk.demo;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class BatchJdbcConnection {

	@Bean
	@Primary
	@ConfigurationProperties(prefix = "spring.datasource")
	public DataSource datasource() {
		return DataSourceBuilder.create().build();
	}

}
