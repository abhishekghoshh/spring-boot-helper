package com.github.typicalitguy.project;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration
public class DatabaseConfiguration {
	

	@Bean
	@ConfigurationProperties(prefix = "spring.mysql.datasource")
	public DataSource mysqlUniversityDatasource() {
		return DataSourceBuilder.create().build();
	}

	@Bean
	public EntityManagerFactory mysqlEntityManagerFactory(
			@Autowired @Qualifier("mysqlUniversityDatasource") DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		localContainerEntityManagerFactoryBean.setDataSource(dataSource);
		localContainerEntityManagerFactoryBean.setPackagesToScan("com.github.typicalitguy.project.mysql.entity");
		localContainerEntityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		localContainerEntityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
		localContainerEntityManagerFactoryBean.afterPropertiesSet();
		return localContainerEntityManagerFactoryBean.getObject();
	}

	@Bean
	@Primary
	public JpaTransactionManager jpaTransactionManager(
			@Autowired @Qualifier("mysqlUniversityDatasource") DataSource dataSource,
			@Autowired @Qualifier("mysqlEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
		jpaTransactionManager.setDataSource(dataSource);
		jpaTransactionManager.setEntityManagerFactory(entityManagerFactory);
		return jpaTransactionManager;
	}
	@Bean
	@ConfigurationProperties(prefix = "spring.postgres.datasource")
	public DataSource postgresUniversityDatasource() {
		return DataSourceBuilder.create().build();
	}

	@Bean
	@Primary
	public EntityManagerFactory postgresqlEntityManagerFactory(
			@Autowired @Qualifier("postgresUniversityDatasource") DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		localContainerEntityManagerFactoryBean.setDataSource(dataSource);
		localContainerEntityManagerFactoryBean.setPackagesToScan("com.github.typicalitguy.project.postgres.entity");
		localContainerEntityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		localContainerEntityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
		localContainerEntityManagerFactoryBean.afterPropertiesSet();
		return localContainerEntityManagerFactoryBean.getObject();
	}
}
