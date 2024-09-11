package com.github.typicalitguy.chunk.demo;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import com.github.typicalitguy.logger.Logger;

public class StudentJobExecutionListner implements JobExecutionListener {

	@Override
	public void beforeJob(JobExecution jobExecution) {
		Logger.log("Before job execution listener is working for job name: " + jobExecution.getJobInstance().getJobName());
		jobExecution.getExecutionContext().put("jobStatus", "working");
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		Logger.log("After job execution listener is working for job name : " + jobExecution.getJobInstance().getJobName());
		Logger.log("jobExecution.getExecutionContext() : " + jobExecution.getExecutionContext());

	}

}
