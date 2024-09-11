package com.github.typicalitguy.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class JobService {
	@Autowired
	private JobLauncher jobLauncher;
	@Autowired
	private ApplicationContext appContext;
	@Autowired
	private JobOperator jobOperator;

	@Async
	public void startJob(String jobName, JobParameters jobParameter)
			throws BeansException, JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		jobLauncher.run((Job) appContext.getBean(jobName), jobParameter);
	}

	public void stopJob(long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		jobOperator.stop(jobExecutionId);
	}

	public JobParameters jobParameters(String uuid, String jobDate) {
		Map<String, JobParameter> params = new HashMap<>();
		params.put("uuid", new JobParameter(uuid));
		params.put("jobDate", new JobParameter(jobDate));
		return new JobParameters(params);
	}

	public Map<String, String> successMap(String uuid) {
		Map<String, String> successMap = new HashMap<>();
		successMap.put("message", "Job Strated");
		successMap.put("startTime", new Date().toString());
		successMap.put("uuid", uuid);
		return successMap;
	}
}
