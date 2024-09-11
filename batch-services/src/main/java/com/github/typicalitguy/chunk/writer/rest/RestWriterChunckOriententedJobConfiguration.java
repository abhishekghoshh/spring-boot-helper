package com.github.typicalitguy.chunk.writer.rest;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.adapter.ItemWriterAdapter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;

import com.github.typicalitguy.chunk.reader.jdbc.StudentJDBC;
import com.github.typicalitguy.chunk.reader.rest.StudentRest;
import com.github.typicalitguy.chunk.reader.rest.StudentRestService;

@Configuration
public class RestWriterChunckOriententedJobConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	@Autowired
	StudentRestService studentRestService;
	
	@Autowired
	@Qualifier("mysqlUniversityDatasource")
	private DataSource universitydataSource;
	
//	@Bean
	public Job restItemWriterChunkOrientedJob() {
		return jobBuilderFactory.get("first rest item writer chunk oriented job")
				.incrementer(new RunIdIncrementer())
				.start(restItemWriterChunkOrientedStep())
				.build();
	}

	private Step restItemWriterChunkOrientedStep() {
		return stepBuilderFactory.get("first rest item writer chunk oriented step")
				.<StudentJDBC, StudentRest>chunk(3)
				.reader(jdbcItemReader())
				.processor(processor())
				.writer(itemWriterAdapter())
				.build();
	}
	

	private JdbcCursorItemReader<StudentJDBC> jdbcItemReader(){
		JdbcCursorItemReader<StudentJDBC> jdbcItemReader = new JdbcCursorItemReader<>();
		jdbcItemReader.setDataSource(universitydataSource);
		jdbcItemReader.setSql("select * from students");
		jdbcItemReader.setRowMapper(rowMapper());
		return jdbcItemReader;
	}
	private RowMapper<StudentJDBC> rowMapper() {
		BeanPropertyRowMapper<StudentJDBC> rowMapper = new BeanPropertyRowMapper<>();
		rowMapper.setMappedClass(StudentJDBC.class);
		return rowMapper;
	}
	private ItemWriterAdapter<StudentRest> itemWriterAdapter() {
		ItemWriterAdapter<StudentRest> itemWriterAdapter = new ItemWriterAdapter<>();
		itemWriterAdapter.setTargetObject(studentRestService);
		itemWriterAdapter.setTargetMethod("addStudent");
		return itemWriterAdapter;
	}

	private ItemProcessor<StudentJDBC,StudentRest> processor() {
		return studentJdbc->{
			StudentRest studentRest = new StudentRest();
			studentRest.setId(studentJdbc.getId());
			studentRest.setFirstName(studentJdbc.getFirstName());
			studentRest.setLastName(studentJdbc.getLastName());
			studentRest.setEmail(studentJdbc.getEmail());
			return studentRest;
		};
	}
	
}
