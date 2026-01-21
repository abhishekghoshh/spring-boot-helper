package com.github.typicalitguy.chunk.writer.csv;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;

import com.github.typicalitguy.chunk.reader.jdbc.StudentJDBC;

@Configuration
public class CsvWriterChunckOriententedJobConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	
	@Autowired
	@Qualifier("mysqlUniversityDatasource")
	private DataSource universitydataSource;
	
//	@Bean
	public Job flatFileWriterChunkOrientedJob() {
		return jobBuilderFactory.get("first flat item writer chunk oriented job")
				.incrementer(new RunIdIncrementer())
				.start(firstJdbcChunkOrientedStep())
				.build();
	}

	private Step firstJdbcChunkOrientedStep() {
		return stepBuilderFactory.get("first flat item writer chunk oriented step")
				.<StudentJDBC, StudentJDBC>chunk(3)
				.reader(jdbcItemReader())
				.writer(flatFileItemWriter())
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
	
	private FlatFileItemWriter<StudentJDBC> flatFileItemWriter(){
		FlatFileItemWriter<StudentJDBC> flatFileItemWriter = new FlatFileItemWriter<>();
		flatFileItemWriter.setResource(absoluteResourcePath("/output-files/csv/students.csv"));
		flatFileItemWriter.setHeaderCallback(flatFileHeaderCallback());
		flatFileItemWriter.setLineAggregator(delimitedLineAggregator());
		flatFileItemWriter.setFooterCallback(flatFileFooterCallback());
		return flatFileItemWriter;
	}


	private DelimitedLineAggregator<StudentJDBC> delimitedLineAggregator() {
		BeanWrapperFieldExtractor<StudentJDBC> beanWrapperFieldExtractor =new BeanWrapperFieldExtractor<>();
		beanWrapperFieldExtractor.setNames(new String[] {"id","firstName","lastName","email"});
		
		DelimitedLineAggregator<StudentJDBC> delimitedLineAggregator= new DelimitedLineAggregator<>();
		delimitedLineAggregator.setFieldExtractor(beanWrapperFieldExtractor);
		delimitedLineAggregator.setDelimiter(",");
		return delimitedLineAggregator;
	}

	private FlatFileHeaderCallback flatFileHeaderCallback() {
		return new FlatFileHeaderCallback() {
			@Override
			public void writeHeader(Writer writer) throws IOException {
				writer.write("id,firstName,lastName,email");
			}
		};
	}
	private FlatFileFooterCallback flatFileFooterCallback() {
		return new FlatFileFooterCallback() {
			@Override
			public void writeFooter(Writer writer) throws IOException {
				writer.write("END,END,END,END");
			}
		};
	}

	private FileSystemResource absoluteResourcePath(String path) {
		return new FileSystemResource(Paths
				.get(System.getProperty("user.dir"), path).toString());
	}
	
}
