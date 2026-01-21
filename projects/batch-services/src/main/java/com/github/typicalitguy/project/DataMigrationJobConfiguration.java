package com.github.typicalitguy.project;

import javax.persistence.EntityManagerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;

import com.github.typicalitguy.project.mysql.entity.StudentMySQL;
import com.github.typicalitguy.project.postgres.entity.StudentPostgres;

@Configuration
public class DataMigrationJobConfiguration {

//	@Bean
	public Job dbMigrationJob(@Autowired JobBuilderFactory jobBuilderFactory,
			@Autowired @Qualifier("dbMigrationStep") Step step) {
		return jobBuilderFactory.get("DB migration project").incrementer(new RunIdIncrementer()).start(step).build();
	}

	@Bean
	public Step dbMigrationStep(@Autowired StepBuilderFactory stepBuilderFactory,
			@Autowired JpaTransactionManager jpaTransactionManager,
			@Autowired @Qualifier("jpaPostgresCursorItemReader") JpaCursorItemReader<StudentPostgres> jpaCursorItemReader,
			@Autowired @Qualifier("jpaMysqlItemWriter") JpaItemWriter<StudentMySQL> jpaItemWriter) {
		return stepBuilderFactory.get("DB migration Step")
				.<StudentPostgres,StudentMySQL>chunk(3)
				.reader(jpaCursorItemReader)
				.processor(processor())
				.writer(jpaItemWriter)
				.faultTolerant()
				.skip(Throwable.class)
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.transactionManager(jpaTransactionManager)
				.build();
	}

	
	@StepScope
	@Bean
	public JpaCursorItemReader<StudentPostgres> jpaPostgresCursorItemReader(
			@Value("#{jobParameters['currentItemCount']}") Integer currentItemCount,
			@Value("#{jobParameters['maxItemCount']}") Integer maxItemCount,
			@Autowired @Qualifier("postgresqlEntityManagerFactory") EntityManagerFactory postgresqlEntityManagerFactory) {
		JpaCursorItemReader<StudentPostgres> jpaCursorItemReader = new JpaCursorItemReader<>();
		jpaCursorItemReader.setEntityManagerFactory(postgresqlEntityManagerFactory);
		jpaCursorItemReader.setQueryString("From StudentPostgres");
		jpaCursorItemReader.setCurrentItemCount(currentItemCount);
		jpaCursorItemReader.setMaxItemCount(maxItemCount);
		return jpaCursorItemReader;
	}

	@StepScope
	@Bean
	public JpaItemWriter<StudentMySQL> jpaMysqlItemWriter(
			@Autowired @Qualifier("mysqlEntityManagerFactory") EntityManagerFactory mysqlEntityManagerFactory) {
		JpaItemWriter<StudentMySQL> jpaItemWriter = new JpaItemWriter<>();
		jpaItemWriter.setEntityManagerFactory(mysqlEntityManagerFactory);
		return jpaItemWriter;
	}

	private ItemProcessor<StudentPostgres, StudentMySQL> processor() {
		return studentPostgres -> new StudentMySQL(studentPostgres.getId(), studentPostgres.getFirstName(),
				studentPostgres.getLastName(), studentPostgres.getEmail(), studentPostgres.getDeptId(),
				null != studentPostgres.getIsActive() && Boolean.valueOf(studentPostgres.getIsActive()));
	}

}
