package it.polimi.diceH2020.launcher.model;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class InteractiveExperiment {
	
	private  Integer id;              
	
	private String instanceName;
	
	@NotNull
	@Min(1)
	private  Integer thinkTime;     	
	@NotNull
	@Min(1)
	private  Integer numUsers;	
	@NotNull
	@Min(1)
	private Integer numVMs;
	
	@NotNull
	@Min(1)
	private Integer iter;

	public InteractiveExperiment(){
	}
}