package com.github.typicalitguy.chunk.reader.rest;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.adapter.ItemReaderAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestReaderChunckOriententedJobConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	StudentRestService studentRestService;

//	@Bean
	public Job flatFileReaderChunkOrientedJob() {
		return jobBuilderFactory.get("first rest item reader chunk oriented job")
				.incrementer(new RunIdIncrementer())
				.start(firstRestChunkOrientedStep())
				.build();
	}

	private Step firstRestChunkOrientedStep() {
		return stepBuilderFactory.get("first rest item reader chunk oriented step")
				.<StudentRest, StudentRest>chunk(3)
				.reader(itemReaderAdapter())
				.writer(itemWriter())
				.build();
	}
	private ItemReaderAdapter<StudentRest> itemReaderAdapter(){
		ItemReaderAdapter<StudentRest> itemReaderAdapter = new ItemReaderAdapter<>();
		itemReaderAdapter.setTargetObject(studentRestService);
		itemReaderAdapter.setTargetMethod("getStudent");
		itemReaderAdapter.setArguments(new Object[] {"Inside getStudent method"});
		return itemReaderAdapter;
	}
	

	private ItemWriter<StudentRest> itemWriter() {
		return new ItemWriter<>() {

			@Override
			public void write(List<? extends StudentRest> items) throws Exception {
				System.out.println(items);
			}

		};
	}
}
