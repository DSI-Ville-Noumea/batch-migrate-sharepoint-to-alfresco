package nc.noumea.mairie.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;

public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

	private static final Logger logger = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

	@Autowired
	public JobCompletionNotificationListener() {
	}
	
	@Override
	public void beforeJob(JobExecution jobExecution) {
		logger.info("execute beforeJob()");
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		if(jobExecution.getStatus() == BatchStatus.COMPLETED) {
			logger.info("!!! JOB FINISHED! Time to verify the results");
		} else {
			logger.info("!!! JOB FINISHED with error! Status : " + jobExecution.getStatus());
		}
	}

}
