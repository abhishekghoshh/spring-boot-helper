package com.github.typicalitguy.schedular;

import java.util.Date;
import java.util.UUID;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.github.typicalitguy.service.JobService;

@Service
public class JobSchedular {
	@Autowired
	JobService jobService;

//	@Scheduled(cron = "0 0/1 * 1/1 * ?")
	public void startJob() throws BeansException, JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		String uuid = UUID.randomUUID().toString();
		JobParameters jobParameter = jobService.jobParameters(uuid, new Date().toString());
		jobService.startJob("firstTaskletJob", jobParameter);
	}
}
