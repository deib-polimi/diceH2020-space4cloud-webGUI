package it.polimi.diceH2020.launcher;

import it.polimi.diceH2020.launcher.utility.FileUtility;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.log4j.Logger;
import org.h2.server.web.WebServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableRetry
@SpringBootApplication
@ComponentScan({"it.polimi.diceH2020.*" })
@EntityScan("it.polimi.diceH2020.launcher.model")
@EnableJpaRepositories("it.polimi.diceH2020.launcher.repository")
@EnableAsync
@EnableScheduling
public class LauncherApplication {

	private static final int uploadTomcatMaxSize = -1; //for a correct redirect disable tomcat. set -1 to disable preupload check
	private static Logger logger = Logger.getLogger(LauncherApplication.class.getName());

	@Autowired
	private FileUtility fileUtility;

	@EventListener
	public void handleContextRefresh(ContextRefreshedEvent event) throws Exception {
		try {
			fileUtility.createWorkingDir();
		} catch (Exception e) {
			logger.info("Error in the creation of local work directory!");
		}
	}

	@Bean
	public TomcatEmbeddedServletContainerFactory containerFactory() {
		return new TomcatEmbeddedServletContainerFactory() {
			protected void customizeConnector(Connector connector) {
				super.customizeConnector(connector);
				if (connector.getProtocolHandler() instanceof AbstractHttp11Protocol) {
					((AbstractHttp11Protocol <?>) connector.getProtocolHandler()).setMaxSwallowSize(uploadTomcatMaxSize);
				}
			}
		};
	}

	@Bean
	public ServletRegistrationBean h2servletRegistration() {
		ServletRegistrationBean registration = new ServletRegistrationBean(new WebServlet());
		registration.addUrlMappings("/console/*");
		return registration;
	}

	public static void main(String[] args) {
		SpringApplication.run(LauncherApplication.class, args);
	}
}
