package com.github.typicalitguy.chunk.reader.json;

import java.nio.file.Paths;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class JSONReaderChunckOriententedJobConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

//	@Bean
	public Job flatFileReaderChunkOrientedJob() {
		return jobBuilderFactory.get("first JSON reader chunk oriented job")
				.incrementer(new RunIdIncrementer())
				.start(firstDemoChunkOrientedStep())
				.build();
	}

	private Step firstDemoChunkOrientedStep() {
		return stepBuilderFactory.get("first JSON reader chunk oriented step")
				.<StudentJSON, StudentJSON>chunk(3)
				.reader(jsonItemReader())
				.writer(itemWriter())
				.build();
	}

	public JsonItemReader<StudentJSON> jsonItemReader() {
		JsonItemReader<StudentJSON> jsonItemReader = new JsonItemReader<>();
		jsonItemReader.setResource(absoluteResourcePath("/input-files/json/students.json"));
		jsonItemReader.setJsonObjectReader(new JacksonJsonObjectReader<>(StudentJSON.class));
		//jsonItemReader.setMaxItemCount(8);
		//jsonItemReader.setCurrentItemCount(2);
		return jsonItemReader;
	}

	private FileSystemResource absoluteResourcePath(String path) {
		return new FileSystemResource(Paths
				.get(System.getProperty("user.dir"), path).toString());
	}

	private ItemWriter<StudentJSON> itemWriter() {
		return new ItemWriter<>() {

			@Override
			public void write(List<? extends StudentJSON> items) throws Exception {
				System.out.println(items);
			}

		};
	}
}
