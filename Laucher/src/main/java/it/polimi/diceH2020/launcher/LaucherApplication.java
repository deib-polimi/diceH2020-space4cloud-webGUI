package it.polimi.diceH2020.launcher;

import org.h2.server.web.WebServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("it.polimi.diceH2020.launcher.model")
@EnableJpaRepositories("it.polimi.diceH2020.launcher.repository")
public class LaucherApplication{
	
	@Bean
	public ServletRegistrationBean h2servletRegistration() {
	    ServletRegistrationBean registration = new ServletRegistrationBean(new WebServlet());
	    registration.addUrlMappings("/console/*");
	    return registration;
	}

	public static void main(String[] args) {
		SpringApplication.run(LaucherApplication.class, args);		
	
		
	}
}
