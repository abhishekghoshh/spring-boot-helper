package com.github.typicalitguy.chunk.reader.jdbc;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;

@Configuration
public class JDBCReaderChunckOriententedJobConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	
	@Autowired
	@Qualifier("mysqlUniversityDatasource")
	private DataSource universitydataSource;
	
//	@Bean
	public Job flatFileReaderChunkOrientedJob() {
		return jobBuilderFactory.get("first jdbc item reader chunk oriented job")
				.incrementer(new RunIdIncrementer())
				.start(firstJdbcChunkOrientedStep())
				.build();
	}

	private Step firstJdbcChunkOrientedStep() {
		return stepBuilderFactory.get("first jdbc reader chunk oriented step")
				.<StudentJDBC, StudentJDBC>chunk(3)
				.reader(jdbcItemReader())
				.writer(itemWriter())
				.build();
	}
	private JdbcCursorItemReader<StudentJDBC> jdbcItemReader(){
		JdbcCursorItemReader<StudentJDBC> jdbcItemReader = new JdbcCursorItemReader<>();
		jdbcItemReader.setDataSource(universitydataSource);
		jdbcItemReader.setSql("select * from students");
		jdbcItemReader.setRowMapper(rowMapper());
//		jdbcItemReader.setMaxItemCount(8);
//		jdbcItemReader.setCurrentItemCount(2);
		return jdbcItemReader;
	}
	private RowMapper<StudentJDBC> rowMapper() {
		BeanPropertyRowMapper<StudentJDBC> rowMapper = new BeanPropertyRowMapper<>();
		rowMapper.setMappedClass(StudentJDBC.class);
		return rowMapper;
	}

	private ItemWriter<StudentJDBC> itemWriter() {
		return new ItemWriter<>() {

			@Override
			public void write(List<? extends StudentJDBC> items) throws Exception {
				System.out.println(items);
			}

		};
	}
}
