package com.github.typicalitguy.chunk.writer.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;

import com.github.typicalitguy.chunk.reader.jdbc.StudentJDBC;

@Configuration
public class JdbcWriterChunckOriententedJobConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	
	@Autowired
	@Qualifier("mysqlUniversityDatasource")
	private DataSource universitydataSource;
	
//	@Bean
	public Job jdbcItemWriterChunkOrientedJob() {
		return jobBuilderFactory.get("first JDBC item writer chunk oriented job")
				.incrementer(new RunIdIncrementer())
				.start(jdbcItemChunkOrientedStep())
				.build();
	}

	private Step jdbcItemChunkOrientedStep() {
		return stepBuilderFactory.get("first JDBC item writer chunk oriented step")
				.<StudentJDBC, StudentJDBC>chunk(3)
				.reader(jdbcItemReader())
				.writer(jdbcBatchItemWriter())
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
	
	@Bean
	public JdbcBatchItemWriter<StudentJDBC> jdbcBatchItemWriter(){
		JdbcBatchItemWriter<StudentJDBC> jdbcBatchItemWriter = new JdbcBatchItemWriter<>();
		jdbcBatchItemWriter.setDataSource(universitydataSource);
		jdbcBatchItemWriter.setSql("insert into students_batch_writter(id,first_name,last_name,email) "
				+ "values (:id,:firstName,:lastName,:email)");
		jdbcBatchItemWriter.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
		return jdbcBatchItemWriter;
	}
	@Bean
	public JdbcBatchItemWriter<StudentJDBC> jdbcBatchItemWriterWithPrepareStatement(){
		JdbcBatchItemWriter<StudentJDBC> jdbcBatchItemWriter = new JdbcBatchItemWriter<>();
		jdbcBatchItemWriter.setDataSource(universitydataSource);
		jdbcBatchItemWriter.setSql("insert into students_batch_writter(id,first_name,last_name,email) "
				+ "values (?,?,?,?)");
		jdbcBatchItemWriter.setItemPreparedStatementSetter( new ItemPreparedStatementSetter<StudentJDBC>() {
			@Override
			public void setValues(StudentJDBC studentJDBC, PreparedStatement ps) throws SQLException {
				ps.setString(1, studentJDBC.getId());
				ps.setString(2, studentJDBC.getFirstName());
				ps.setString(3, studentJDBC.getLastName());
				ps.setString(4, studentJDBC.getEmail());
			}
		});
		return jdbcBatchItemWriter;
	}
}
