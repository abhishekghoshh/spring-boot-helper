package com.github.typicalitguy.chunk.demo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StudentJobConfiguration {
//	@Bean
	public Job studentChunkOrientedJob(@Autowired JobBuilderFactory jobBuilderFactory,
			@Autowired StepBuilderFactory stepBuilderFactory) {
		return jobBuilderFactory.get("student-chunk-oriented-job")
				.incrementer(new RunIdIncrementer())
				.start(studentChunkOrientedStep(stepBuilderFactory))
				.next(professorTakletStep(stepBuilderFactory))
				.next(studentJobCompletionTakletStep(stepBuilderFactory))
				.listener(new StudentJobExecutionListner())
				.build();
	}
	private Step studentChunkOrientedStep(StepBuilderFactory stepBuilderFactory) {
		return stepBuilderFactory.get("student-chunk-oriented-step")
				.<Student, Student>chunk(2)
				.reader(new StudentItemReader())
				.processor(new StudentItemProcessor())
				.writer(new StudentItemWriter())
				.listener(new StudentStepExecutionListener())
				.build();
	}
	private Step professorTakletStep(StepBuilderFactory stepBuilderFactory) {
		return stepBuilderFactory.get("proffesor-notifier-tasklet-step")
				.tasklet(new ProfessorNotifierTasklet())
				.build();
	}
	private Step studentJobCompletionTakletStep(StepBuilderFactory stepBuilderFactory) {
		return stepBuilderFactory.get("student-job-completion-tasklet-step")
				.tasklet(new StudentJobCompletionTasklet())
				.build();
	}
}
