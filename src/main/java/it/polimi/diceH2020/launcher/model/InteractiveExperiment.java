package it.polimi.diceH2020.launcher.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.launcher.SimulationsStates;
import it.polimi.diceH2020.launcher.utility.Compressor;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.IOException;

@Entity
@Data
public class InteractiveExperiment {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@NotNull
	private String instanceName;

	@ManyToOne
	@JoinColumn(name = "SIM_MANAGER")//, updatable = false, insertable=false, nullable = false)
	private SimulationsManager simulationsManager;

	private String simType;

	//@NotNull
	@Min(1)
	private  Integer thinkTime;
	//@NotNull
	private  Integer numUsers;
	//@NotNull
	private Integer numVMs;

	private double responseTime;

	private Integer gamma;

	private String provider;

	private int numSolutions;

	@Column(length = 1000)
	@NotNull
	private String finalSolution;

	//@NotNull
	private Integer iter;

	private long experimentalDuration;

	private SimulationsStates state;
	//@NotNull
	private boolean done; //TODO is it used?

	public InteractiveExperiment(){
		thinkTime = 1;
		numUsers=0;
		numVMs=0;
		responseTime = 0d;
		gamma = 0;
		numSolutions = 0;
		iter = 1;
		experimentalDuration = 0;
		
		finalSolution= new String();
		instanceName = new String();
		provider = "NONE";
		
		state = SimulationsStates.READY;
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

	public void setSol(Solution sol){
		ObjectMapper mapper = new ObjectMapper();
		try {
			this.finalSolution = Compressor.compress(mapper.writeValueAsString(sol));
		} catch ( IOException e) {
			this.finalSolution = "Error";
		}
	}
	
	public Solution getSol(){
		ObjectMapper mapper = new ObjectMapper();
		try {
			return this.finalSolution.equals("") || this.finalSolution.equals("Error")? null :
					mapper.readValue(Compressor.decompress(this.finalSolution), Solution.class);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
