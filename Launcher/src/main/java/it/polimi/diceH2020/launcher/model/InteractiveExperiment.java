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

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import lombok.Data;

@Entity
@Data
public class InteractiveExperiment {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;           
	@NotNull
	private String instanceName = "";
	
	@ManyToOne(cascade = {CascadeType.ALL})
	@JoinColumn(name = "SIM_MANAGER")//, updatable = false, insertable=false, nullable = false)
	private SimulationsManager simulationsManager;
	
	//private String simType;
	
	//@NotNull
	@Min(1)
	private  Integer thinkTime;     	
	//@NotNull
	@Min(1)
	private  Integer numUsers;	
	//@NotNull
	@Min(1)
	private Integer numVMs;
	
	private double responseTime = 0d;
	
	private Integer gamma;
	
	private String provider = "";
	
	private int numSolutions = 0;
	
	//@NotNull
	@Min(1)
	private Integer iter = 1;
	
	private long experimentalDuration = 0;

	private String state = "ready"; //states:  ready to be executed, running, completed, failed
	//@NotNull
	private boolean done;
	
	public InteractiveExperiment(){
	}

	public Solution getInputSolution()  {
		if(simulationsManager instanceof SimulationsWIManager){
			SimulationsWIManager wiM = (SimulationsWIManager)simulationsManager;
			Solution sol = wiM.getInputJson();
			SolutionPerJob spj = sol.getLstSolutions().get(0);
			spj.getJob().setThink(thinkTime);
			spj.setNumberVM(numVMs);
			spj.setNumberUsers(numUsers);
			return sol;
		}
		throw new ClassCastException();
	}
	
	public InstanceData getInputData(){
		if(simulationsManager instanceof SimulationsOptManager){
			SimulationsOptManager wiM = (SimulationsOptManager)simulationsManager;
			InstanceData data = wiM.getInputData();
			return data;
		}
		throw new ClassCastException();
	}
	
}