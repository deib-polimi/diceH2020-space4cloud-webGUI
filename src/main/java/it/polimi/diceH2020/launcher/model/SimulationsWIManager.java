package it.polimi.diceH2020.launcher.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.SolverType;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.launcher.utility.Compressor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.IOException;

/**
 * This class contain informations about client's requested set of simulations
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class SimulationsWIManager extends SimulationsManager{

	@NotNull
	private Integer accuracy;

	@Transient
	private Solution inputJson;

	@Transient
	private Integer maxNumUsers;
	@Transient
	private Integer maxNumVMs;
	@Transient
	private Integer minNumUsers;
	@Transient
	private Integer minNumVMs;

	private Integer numIter;

	private SolverType solver;

	@NotNull
	@Transient
	private Integer stepUsers;

	@NotNull
	@Transient
	private Integer stepVMs;

	@Transient
	private Integer thinkTime;

	@NotNull
	@Min(60)
	private Integer simDuration;

	public SimulationsWIManager() {
		super();
		setType("WI");

		solver = SolverType.QNSolver;

		simDuration = 60;
		stepVMs = 1;
		stepUsers = 1;
		numIter = 1;
		accuracy = 5;
		maxNumUsers = 1;
		maxNumVMs = 1;
		minNumUsers = 1;
		minNumVMs = 1;
	}

	public void buildExperiments() {
		getExperimentsList().clear();
		for (int numVMs = minNumVMs; numVMs <= maxNumVMs; numVMs = numVMs + stepVMs)
			for (int numUsers = minNumUsers; numUsers <= maxNumUsers; numUsers = numUsers + stepUsers)
				for (int it = 1; it <= this.numIter; it++) {
					InteractiveExperiment experiment = new InteractiveExperiment();
					experiment.setIter(it);
					experiment.setNumUsers(numUsers);
					experiment.setNumVMs(numVMs);
					experiment.setThinkTime(this.thinkTime);
					experiment.setInstanceName(getInstanceName());
					experiment.setSimulationsManager(this);
					experiment.setSimType("WI");
					getExperimentsList().add(experiment);
				}
	}

	public Solution getDecompressedInputJson() {
		if (inputJson != null) {
			return inputJson;
		} else if (getInput() != null) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				return getInput().equals("") || getInput().equals("Error") ? null :
						mapper.readValue(Compressor.decompress(getInput()), Solution.class);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return inputJson;
	}

	public void setInputJson(Solution inputSolution) {
		this.inputJson = inputSolution;

		ObjectMapper mapper = new ObjectMapper();
		try {
			setInput(Compressor.compress(mapper.writeValueAsString(inputSolution)));
		} catch (IOException e) {
			setInput("Error");
		}
		Double tt = inputSolution.getLstSolutions().get(0).getJob().getThink();
		this.thinkTime = tt.intValue();
		setInstanceName(inputSolution.getId());
	}
}
