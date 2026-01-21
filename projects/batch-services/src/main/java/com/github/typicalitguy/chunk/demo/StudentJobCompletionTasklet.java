package com.github.typicalitguy.chunk.demo;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import com.github.typicalitguy.logger.Logger;

public class StudentJobCompletionTasklet implements Tasklet {

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		Logger.log("Step execution context is " +chunkContext.getStepContext().getStepExecutionContext());
		Logger.log("Student job is completed");
		return RepeatStatus.FINISHED;
	}

}
