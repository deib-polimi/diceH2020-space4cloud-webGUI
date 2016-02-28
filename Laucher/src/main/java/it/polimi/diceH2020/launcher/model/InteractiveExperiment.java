package it.polimi.diceH2020.launcher.model;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Entity
@Data
public class InteractiveExperiment {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;           
	
	private String instanceName;
	
	@ManyToOne(cascade = {CascadeType.ALL})
	@JoinColumn(name = "SIM_MANAGER")//, updatable = false, insertable=false, nullable = false)
	private SimulationsManager simulationsManager;
	
	@NotNull
	@Min(1)
	private  Integer thinkTime = 1;     	
	@NotNull
	@Min(1)
	private  Integer numUsers = 1;	
	@NotNull
	@Min(1)
	private Integer numVMs = 1;
	
	@NotNull
	@Min(1)
	private Integer iter = 1;

	
	private long experimentalDuration = 0;

	private double responseTime = 0d;
	
	private String state = "ready"; //states:  ready to be executed, running, completed, failed
	
	public InteractiveExperiment(){
	}
}