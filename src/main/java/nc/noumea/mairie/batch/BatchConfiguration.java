package nc.noumea.mairie.batch;

import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import nc.noumea.mairie.alfresco.cmis.CreateSession;
import nc.noumea.mairie.eae.domain.Eae;
import nc.noumea.mairie.eae.domain.EaeEvalue;
import nc.noumea.mairie.eae.domain.EaeFinalisation;
import nc.noumea.mairie.writer.EaeWriter;

import org.hibernate.SessionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.HibernateCursorItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

/**
 * This class contains all the configuration of the Spring Batch application
 * used for this tutorial. It contains readers, writers, processors, jobs, steps
 * and all the needed beans.
 * 
 * @author dgutierrez-diez
 *
 */
@Configuration
@EnableBatchProcessing
@ComponentScan(basePackages = "nc.noumea.mairie")
@ImportResource(value = {"classpath:applicationContext.xml"})
public class BatchConfiguration {

	@Autowired
	private CreateSession createSession;

	/**
	 * START Parametres applicationContext.xml
	 */
	private String alfrescoUrl;
	private String alfrescoLogin;
	private String alfrescoPassword;
	
	private String kiosqueUserWebdav;
	private String kiosqueUserPwsWebdav;
	private String kiosqueDomainWebdav;
	private String kiosqueUrlWebdav;
	private String kiosquePortWebdav;
	private String kiosqueUrlGedSharepoint;
	
	/**
	 * END Parametres applicationContext.xml
	 */

	/* ********************************************
	 * READERS This section contains all the readers
	 * ********************************************
	 */
	/**
	 * The second reader
	 * @return a reader
	 */
	@Bean
	public HibernateCursorItemReader<EaeFinalisation> readerEae(@Qualifier(value = "sessionFactory") SessionFactory sessionFactory) {

		HibernateCursorItemReader<EaeFinalisation> reader = new HibernateCursorItemReader<EaeFinalisation>();
		reader.setSessionFactory(sessionFactory);
		reader.setQueryName("getListEaeFinalisation");
		return reader;
	}

	/* ********************************************
	 * PROCESSORS This section contains all processors
	 * ********************************************
	 */

	/**
	 * 
	 * @return custom item processor -> anything
	 */
//	@Bean
//	public DocumentItemProcessor processor() {
//		return new DocumentItemProcessor();
//	}

	/* ********************************************
	 * WRITERS This section contains all the writers
	 * ********************************************
	 */
	/**
	 * 
	 * @param dataSource
	 * @return item writer custom
	 */
	@Bean
	public EaeWriter<EaeFinalisation> writerEae(@Qualifier(value = "sessionFactory") SessionFactory sessionFactory) {

		EaeWriter<EaeFinalisation> writer = new EaeWriter<EaeFinalisation>();
		writer.setSessionFactory(sessionFactory);
		writer.setAlfrescoUrl(alfrescoUrl);
		writer.setAlfrescoLogin(alfrescoLogin);
		writer.setAlfrescoPassword(alfrescoPassword);
		
		writer.setKiosqueUrlGedSharepoint(kiosqueUrlGedSharepoint);
		writer.setKiosqueUrlWebdav(kiosqueUrlWebdav);
		writer.setKiosquePortWebdav(kiosquePortWebdav);
		writer.setKiosqueUserWebdav(kiosqueUserWebdav);
		writer.setKiosqueUserPwsWebdav(kiosqueUserPwsWebdav);
		writer.setKiosqueDomainWebdav(kiosqueDomainWebdav);
		
		return writer;
	}

	
	/* ********************************************
	 * LISTENER ***********************************
	 * ********************************************
	 */
	@Bean
    public JobExecutionListener listener() {
        return new JobCompletionNotificationListener();
    }

	/* ********************************************
	 * JOBS ***************************************
	 * ********************************************
	 */
	/**
	 * 
	 * @param jobs
	 * @param s1
	 *            steps
	 * @param s2
	 *            steps
	 * @return the Job
	 */
	@Bean
	public Job importDocumentSIRH(JobBuilderFactory jobs, 
			@Qualifier("stepMigrateEae") Step stepMigrateEae
			) {
		return jobs.get("importDocumentSIRH").incrementer(new RunIdIncrementer())
                .listener(listener())
                .flow(stepMigrateEae)
                .end().build();
	}

	/* ********************************************
	 * STEPS **************************************
	 * ********************************************
	 */
	
	/**
	 * Migre les EAEs de sharepoint vers Alfresco
	 * et met à jour l URL dans la BDD EAE
	 * 
	 * @param stepBuilderFactory
	 * @param reader
	 * @param writer
	 * @param processor
	 * @return Step
	 */
	@Bean
	public Step stepMigrateEae(StepBuilderFactory stepBuilderFactory,
			@Qualifier("writerEae") ItemWriter<EaeFinalisation> writerEae,
			@Qualifier("readerEae") ItemReader<EaeFinalisation> readerEae) {
		/* it handles bunches of 10 units */
		return stepBuilderFactory.get("stepMigrateEae")
				.<EaeFinalisation, EaeFinalisation> chunk(20).reader(readerEae)
				.writer(writerEae).build();
	}

	/* ********************************************
	 * UTILITY BEANS ******************************
	 * ********************************************
	 */

	/**
	 * jdbc template (hsqldb)
	 * 
	 * @param dataSource
	 * @return JdbcTemplate
	 */
	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}
	
	@Bean
	public ResourcelessTransactionManager transactionManager() {
		return new ResourcelessTransactionManager();
	}
	
	@Autowired
	private Environment env;
	

	@Primary
	@Bean(name = "dataSource")
	public DataSource dataSource() {

		String dataSourceReaderDriver = env
				.getProperty("dataSourceLogBatch.driverClassName");
		String dataSourceReaderUrl = env.getProperty("dataSourceLogBatch.url");
		String dataSourceReaderUsername = env
				.getProperty("dataSourceLogBatch.username");
		String dataSourceReaderPassword = env
				.getProperty("dataSourceLogBatch.password");

		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(dataSourceReaderDriver);
		dataSource.setUrl(dataSourceReaderUrl);
		dataSource.setUsername(dataSourceReaderUsername);
		dataSource.setPassword(dataSourceReaderPassword);
		return dataSource;
	}
	
	@Bean
	BatchConfigurer configurer(DataSource dataSource){
	  return new DefaultBatchConfigurer(dataSource);
	}

	@Bean(name = "dataSourceEae")
	public DataSource dataSourceEae() {

		String dataSourceEaeDriver = env
				.getProperty("dataSourceEae.driverClassName");
		String dataSourceEaeUrl = env
				.getProperty("dataSourceEae.url");
		String dataSourceEaeUsername = env
				.getProperty("dataSourceEae.username");
		String dataSourceEaePassword = env
				.getProperty("dataSourceEae.password");

		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(dataSourceEaeDriver);
		dataSource.setUrl(dataSourceEaeUrl);
		dataSource.setUsername(dataSourceEaeUsername);
		dataSource.setPassword(dataSourceEaePassword);
		return dataSource;
	}
	
	@Bean(name = "sessionFactory")
	public SessionFactory sessionFactory(@Qualifier("dataSourceEae") DataSource dataSourceEae) throws IOException {
		LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
		sessionFactory.setDataSource(dataSourceEae);
		sessionFactory.setAnnotatedPackages("nc.noumea.mairie.eae.domain");
		sessionFactory.setAnnotatedClasses(EaeFinalisation.class, Eae.class, EaeEvalue.class);
		sessionFactory.setHibernateProperties(getHibernateProperties());
		sessionFactory.afterPropertiesSet();
		return sessionFactory.getObject();
	}
	
    private static Properties getHibernateProperties() {

        Properties hibernateProperties = new Properties();
        hibernateProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        hibernateProperties.put("hibernate.show_sql", false);
        hibernateProperties.put("hibernate.generate_statistics", false);
        hibernateProperties.put("hibernate.hbm2ddl.auto", "validate");
        hibernateProperties.put("hibernate.use_sql_comments", false);

        return hibernateProperties;
    }

	public String getAlfrescoUrl() {
		return alfrescoUrl;
	}

	public void setAlfrescoUrl(String alfrescoUrl) {
		this.alfrescoUrl = alfrescoUrl;
	}

	public String getAlfrescoLogin() {
		return alfrescoLogin;
	}

	public void setAlfrescoLogin(String alfrescoLogin) {
		this.alfrescoLogin = alfrescoLogin;
	}

	public String getAlfrescoPassword() {
		return alfrescoPassword;
	}

	public void setAlfrescoPassword(String alfrescoPassword) {
		this.alfrescoPassword = alfrescoPassword;
	}

	public String getKiosqueUserWebdav() {
		return kiosqueUserWebdav;
	}

	public void setKiosqueUserWebdav(String kiosqueUserWebdav) {
		this.kiosqueUserWebdav = kiosqueUserWebdav;
	}

	public String getKiosqueUserPwsWebdav() {
		return kiosqueUserPwsWebdav;
	}

	public void setKiosqueUserPwsWebdav(String kiosqueUserPwsWebdav) {
		this.kiosqueUserPwsWebdav = kiosqueUserPwsWebdav;
	}

	public String getKiosqueDomainWebdav() {
		return kiosqueDomainWebdav;
	}

	public void setKiosqueDomainWebdav(String kiosqueDomainWebdav) {
		this.kiosqueDomainWebdav = kiosqueDomainWebdav;
	}

	public String getKiosqueUrlWebdav() {
		return kiosqueUrlWebdav;
	}

	public void setKiosqueUrlWebdav(String kiosqueUrlWebdav) {
		this.kiosqueUrlWebdav = kiosqueUrlWebdav;
	}

	public String getKiosquePortWebdav() {
		return kiosquePortWebdav;
	}

	public void setKiosquePortWebdav(String kiosquePortWebdav) {
		this.kiosquePortWebdav = kiosquePortWebdav;
	}

	public String getKiosqueUrlGedSharepoint() {
		return kiosqueUrlGedSharepoint;
	}

	public void setKiosqueUrlGedSharepoint(String kiosqueUrlGedSharepoint) {
		this.kiosqueUrlGedSharepoint = kiosqueUrlGedSharepoint;
	}

}