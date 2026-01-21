package com.github.typicalitguy.chunk.tolerance;

import java.nio.file.Paths;

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
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import com.github.typicalitguy.chunk.reader.csv.StudentCSV;
import com.github.typicalitguy.chunk.reader.json.StudentJSON;

@Configuration
public class TolerantChunckOriententedJobConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	@Autowired
	SkipListnerWithAnnotation skipListner;

//	@Bean
	public Job toleratFlatFileReaderChunkOrientedJob() {
		return jobBuilderFactory.get("tolerant CSV reader chunk oriented job")
				.incrementer(new RunIdIncrementer())
				.start(tolerantFlatFileChunkOrientedStep())
				.build();
	}

	private Step tolerantFlatFileChunkOrientedStep() {
		return stepBuilderFactory.get("tolerant CSV reader chunk oriented step")
				.<StudentCSV, StudentJSON>chunk(3)
				.reader(flatFileItemReader())
				.processor(processor())
				.writer(jsonFileItemWritter())
				.faultTolerant()
//				.retryLimit(2)
//				.retry(Throwable.class)
				.skip(Throwable.class)
				.skipLimit(Integer.MAX_VALUE)
				//.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.listener(skipListner)
				.build();
	}

	public FlatFileItemReader<StudentCSV> flatFileItemReader() {
		FlatFileItemReader<StudentCSV> flatFileItemReader = new FlatFileItemReader<>();
		flatFileItemReader.setResource(absoluteResourcePath("/input-files/csv/faulty-students.csv"));
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

	private JsonFileItemWriter<StudentJSON> jsonFileItemWritter() {
		return new JsonFileItemWriter<>(absoluteResourcePath("/output-files/json/faulty-students.json"),
				new JacksonJsonObjectMarshaller<>()) {
			@Override
			public String doWrite(java.util.List<? extends StudentJSON> items) {
				items.forEach(student->{
					if(student.getId().equalsIgnoreCase("111118")) {
						throw new RuntimeException();
					}
				});
				return super.doWrite(items);
			};
		};
	}
	private ItemProcessor<StudentCSV,StudentJSON> processor() {
		return  studentCSV ->{
				if(studentCSV.getId().equalsIgnoreCase("111115")) {
					throw new NullPointerException();
				}
				return new StudentJSON(studentCSV.getId(),studentCSV.getFirstName(),studentCSV.getLastName(),studentCSV.getEmail());
			};
	}

	private FileSystemResource absoluteResourcePath(String path) {
		return new FileSystemResource(Paths
				.get(System.getProperty("user.dir"), path).toString());
	}
}
