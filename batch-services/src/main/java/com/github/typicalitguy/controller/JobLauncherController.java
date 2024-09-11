package com.github.typicalitguy.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.typicalitguy.service.JobService;

@RestController
@RequestMapping("/api/job")
public class JobLauncherController {

	@Autowired
	JobService jobService;

	/*
	 * Select * from spring_batch.batch_job_execution where job_execution_id =(
	 * SELECT JOB_EXECUTION_ID FROM spring_batch.batch_job_execution_params where
	 * key_name = "uuid" and string_val = "9aea296b-0a8e-4b79-a778-d5d881d2ee1b" );
	 * 
	 */
	@GetMapping("/start/{jobName}/date/{jobDate}")
	public Map<String, String> startJob(@PathVariable String jobName, @PathVariable String jobDate)
			throws BeansException, JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		String uuid = UUID.randomUUID().toString();
		JobParameters jobParameter = jobService.jobParameters(uuid, jobDate);
		jobService.startJob(jobName, jobParameter);
		return jobService.successMap(uuid);
	}

}
