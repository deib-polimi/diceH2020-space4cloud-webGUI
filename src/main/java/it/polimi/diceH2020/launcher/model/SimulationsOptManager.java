package it.polimi.diceH2020.launcher.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.launcher.Settings;
import it.polimi.diceH2020.launcher.utility.Compressor;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.IOException;

@Entity
@Data
public class SimulationsOptManager extends SimulationsManager{

	private Integer numIter;

	private String provider;

	private Integer gamma;

	@Transient
	private InstanceData inputData;

	public SimulationsOptManager(){
		super();
		Settings set = new Settings();
		this.numIter = set.getNumIterations();
		setType("Opt");
		
		provider = new String();
	}

	public void buildExperiments() {
		super.getExperimentsList().clear();
		for (int it = 1; it <= this.numIter; it++) {
			InteractiveExperiment experiment = new InteractiveExperiment();
			experiment.setIter(it);
			experiment.setInstanceName(getInstanceName());
			experiment.setSimulationsManager(this);
			experiment.setGamma(this.gamma);
			experiment.setProvider(this.provider);
			experiment.setSimType("Opt");
			super.getExperimentsList().add(experiment);
		}
	}

	public void setInputData(InstanceData inputData) {
		this.inputData = inputData;

		ObjectMapper mapper = new ObjectMapper();
		try {
			setInput(Compressor.compress(mapper.writeValueAsString(inputData)));
		} catch (IOException e) {
			setInput("Error");
		}
		setInstanceName(inputData.getId());
	}

	public InstanceData getDecompressedInputData() {
		if (inputData != null) {
			return inputData;
		} else if (getInput() != null) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				return getInput().equals("") || getInput().equals("Error") ? null : mapper.readValue(Compressor.decompress(getInput()), InstanceData.class);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return inputData;
	}
}
