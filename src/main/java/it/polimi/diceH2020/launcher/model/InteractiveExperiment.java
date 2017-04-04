/*
Copyright 2016 Jacopo Rigoli
Copyright 2016 Michele Ciavotta
Copyright 2016 Eugenio Gianniti

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
package it.polimi.diceH2020.launcher.model;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.utility.Compressor;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.io.IOException;

@Entity
@Data
@ToString(exclude = "simulationsManager")
public class InteractiveExperiment {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String instanceName;

	@ManyToOne
	@JoinColumn(name = "SIM_MANAGER")//, updatable = false, insertable=false, nullable = false)
	private SimulationsManager simulationsManager;

	private String provider;

	@Column(length = 100000)

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

	public InteractiveExperiment(){}

	public InstanceDataMultiProvider getInputData(){
		return this.simulationsManager.getDecompressedInputData();
	}

	public void setSol(Solution sol){
		ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module()).enable(SerializationFeature.INDENT_OUTPUT);
		try {
			this.finalSolution = Compressor.compress(mapper.writeValueAsString(sol));
		} catch ( IOException e) {
			this.finalSolution = "Error";
		}
	}

	public Solution getSol() throws JsonParseException, JsonMappingException, IOException{
		ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module()).enable(SerializationFeature.INDENT_OUTPUT);;
		return mapper.readValue(Compressor.decompress(finalSolution),Solution.class);
	}

}
