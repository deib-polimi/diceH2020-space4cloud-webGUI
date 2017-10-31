/*
Copyright 2016 Jacopo Rigoli
Copyright 2016 Michele Ciavotta

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.launcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
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
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry
@SpringBootApplication
@ComponentScan({"it.polimi.diceH2020.*"})
@EntityScan({"it.polimi.diceH2020.launcher.model","it.polimi.diceH2020.SPACE4Cloud.shared.settings"})
@EnableJpaRepositories("it.polimi.diceH2020.launcher.repository")
@EnableAsync
@EnableScheduling
public class LauncherApplication {

	private static final int uploadTomcatMaxSize = -1; //for a correct redirect disable tomcat. set -1 to disable preupload check
	private final Logger logger = Logger.getLogger(getClass ());

	@Autowired
	private FileUtility fileUtility;

	@EventListener
	public void handleContextRefresh(ContextRefreshedEvent event) throws Exception {
		try {
			fileUtility.createWorkingDir();
			logger.info ("Working directory correctly created");
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
	public ServletRegistrationBean h2servletRegistration(){
		ServletRegistrationBean registration = new ServletRegistrationBean(new WebServlet());
		registration.addUrlMappings("/console/*");
		return registration;
	}

	@Bean
	public MappingJackson2HttpMessageConverter jacksonMapper(){
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(objectMapper());
		return converter;
	}

	@Primary
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		registerModules(mapper);
		return mapper;
	}

	@Bean
	public Jdk8Module jdk8Module() {
		return new Jdk8Module();
	}

	private void registerModules(ObjectMapper mapper) {
		mapper.registerModule(jdk8Module());
	}

	@Primary
	@Bean
	public ObjectWriter writer(ObjectMapper mapper) {
		return mapper.writer();
	}

	@Primary
	@Bean
	public ObjectReader reader(ObjectMapper mapper) {
		return mapper.reader();
	}


	public static void main(String[] args) {
		SpringApplication.run(LauncherApplication.class, args);
	}
}
