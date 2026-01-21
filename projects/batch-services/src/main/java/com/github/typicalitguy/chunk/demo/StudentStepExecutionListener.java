package com.github.typicalitguy.chunk.demo;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import com.github.typicalitguy.logger.Logger;

public class StudentStepExecutionListener implements StepExecutionListener {

	@Override
	public void beforeStep(StepExecution stepExecution) {
		Logger.log("Before step execution listener is working for step name :" + stepExecution.getStepName());
		stepExecution.getExecutionContext().put("stepStatus", "Working");
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		Logger.log("After step execution listener is working for step name :" + stepExecution.getStepName());
		Logger.log("stepExecution.getExecutionContext() : " + stepExecution.getExecutionContext());
		return ExitStatus.COMPLETED;
	}

}
