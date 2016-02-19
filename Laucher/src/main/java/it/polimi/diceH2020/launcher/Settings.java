package it.polimi.diceH2020.launcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "launcher")
@Data
public class Settings {
	private String instanceDir;
	private String solInstanceDir;
	private String txtDir;
	private Integer numIterations = 1;
	private String resultDir;
	private String address;
	private String port;
	
	
	
	public String getfullAddress(){
		return "http://"+address+":"+port;
	}

}
