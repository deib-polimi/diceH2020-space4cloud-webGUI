package it.polimi.diceH2020.launcher.model;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.utility.Compressor;
import lombok.Data;

import java.io.IOException;

import javax.persistence.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
@Data
public class InteractiveExperiment {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	private String instanceName;

	@ManyToOne
	@JoinColumn(name = "SIM_MANAGER")//, updatable = false, insertable=false, nullable = false)
	private SimulationsManager simulationsManager;

	private String provider;

	@Column(length = 1000)

	private String finalSolution;

	private long experimentalDuration;

	private States state;

	private boolean done;
	
	private boolean error;
	
	public InteractiveExperiment(String instanceName, String provider, SimulationsManager simManager){
		initializeAttributes();
		
		this.provider = provider;
		this.instanceName = instanceName;
		this.simulationsManager = simManager;
	}
	
	public void initializeAttributes(){
		experimentalDuration = 0;
		finalSolution= "";
		state = States.READY;
		error = false;
		done = false;
	}
	
	public InteractiveExperiment(){
		
	}

	public InstanceData getInputData(){
		return this.simulationsManager.getDecompressedInputData();
	}

	public void setSol(Solution sol){
		ObjectMapper mapper = new ObjectMapper();
		try {
			this.finalSolution = Compressor.compress(mapper.writeValueAsString(sol));
		} catch ( IOException e) {
			this.finalSolution = "Error";
		}
	}
	
	public Solution getSol() throws JsonParseException, JsonMappingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(Compressor.decompress(finalSolution),Solution.class);
		
	}

}
