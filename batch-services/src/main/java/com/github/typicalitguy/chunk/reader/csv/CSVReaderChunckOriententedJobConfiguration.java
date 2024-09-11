package com.github.typicalitguy.chunk.reader.csv;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.github.typicalitguy.chunk.demo.Student;
import com.github.typicalitguy.chunk.demo.StudentItemWriter;
import com.github.typicalitguy.util.PathUtils;

@Configuration
public class CSVReaderChunckOriententedJobConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

//	@Bean
	public Job flatFileReaderChunkOrientedJob() {
		return jobBuilderFactory.get("first flat file CSV reader chunk oriented job")
				.incrementer(new RunIdIncrementer())
				.start(firstDemoChunkOrientedStep())
				.build();
	}

	private Step firstDemoChunkOrientedStep() {
		return stepBuilderFactory.get("first first flat file CSV reader chunk oriented step")
				.<StudentCSV, Student>chunk(3)
				.reader(flatFileItemReader())
				.processor(processor())
				.writer(new StudentItemWriter())
				.build();
	}


	public FlatFileItemReader<StudentCSV> flatFileItemReader() {
		FlatFileItemReader<StudentCSV> flatFileItemReader = new FlatFileItemReader<>();
		flatFileItemReader.setResource(PathUtils.resource("/input-files/csv/students.csv"));
		flatFileItemReader.setLineMapper(createDefaultLineMapper());
		flatFileItemReader.setLinesToSkip(1);
		return flatFileItemReader;
	}
	private DefaultLineMapper<StudentCSV> createDefaultLineMapper() {

		DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
		delimitedLineTokenizer.setDelimiter(",");
		delimitedLineTokenizer.setNames("id", "first_name", "last_name", "email");

		BeanWrapperFieldSetMapper<StudentCSV> beanWrapperFieldSetMapper = new BeanWrapperFieldSetMapper<>();
		beanWrapperFieldSetMapper.setTargetType(StudentCSV.class);

		DefaultLineMapper<StudentCSV> defaultLineMapper = new DefaultLineMapper<>();
		defaultLineMapper.setLineTokenizer(delimitedLineTokenizer);
		defaultLineMapper.setFieldSetMapper(beanWrapperFieldSetMapper);
		return defaultLineMapper;
	}


	private ItemProcessor<? super StudentCSV, ? extends Student> processor() {
		return student -> new Student(student.getId(), student.getFirstName(), student.getLastName(),
				student.getEmail());
	}
}
