package com.github.typicalitguy.chunk.writer.json;

import java.nio.file.Paths;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;

import com.github.typicalitguy.chunk.reader.jdbc.StudentJDBC;

@Configuration
public class JsonWriterChunckOriententedJobConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Autowired
	@Qualifier("mysqlUniversityDatasource")
	private DataSource universitydataSource;

//	@Bean
	public Job flatFileWriterChunkOrientedJob() {
		return jobBuilderFactory.get("first json item writer chunk oriented job").incrementer(new RunIdIncrementer())
				.start(firstJdbcChunkOrientedStep()).build();
	}

	private Step firstJdbcChunkOrientedStep() {
		return stepBuilderFactory.get("first json item writer chunk oriented step").<StudentJDBC, StudentJDBC>chunk(3)
				.reader(jdbcItemReader()).writer(jsonFileItemWritter()).build();
	}

	private JdbcCursorItemReader<StudentJDBC> jdbcItemReader() {
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

	private JsonFileItemWriter<StudentJDBC> jsonFileItemWritter() {
		return new JsonFileItemWriter<>(absoluteResourcePath("/output-files/json/students.json"),
				new JacksonJsonObjectMarshaller<>());
	}
	private FileSystemResource absoluteResourcePath(String path) {
		return new FileSystemResource(Paths
				.get(System.getProperty("user.dir"), path).toString());
	}
}
