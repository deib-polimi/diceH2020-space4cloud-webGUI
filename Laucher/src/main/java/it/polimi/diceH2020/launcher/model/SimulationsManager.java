package it.polimi.diceH2020.launcher.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;


/**
 * This class contain informations about client's requested set of simulations
 */
@Entity
public class SimulationsManager {
	
	@NotNull
	private Integer accuracy = 5;
	
	@Transient
	private List<InteractiveExperiment> classList = new ArrayList<InteractiveExperiment>();
	private String date;
	private String folderPath;
	@Id
	@Column(name = "SIM_MANAGER")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@Transient
	private Solution inputSolution;         
	@Transient
	private  Integer maxNumUsers;
	@Transient
	private  Integer maxNumVMs;
	@Transient
	private  Integer minNumUsers;	
	@Transient
	private  Integer minNumVMs;	
	
	private Integer numCompletedSimulations;
	
	@Transient
	private Integer numIter;
	
	@OneToMany(mappedBy = "simulationsManager", fetch = FetchType.LAZY,cascade=CascadeType.ALL)
	@OrderColumn(name = "simManager_index")
	private List<Simulation> simulationsList = new ArrayList<Simulation>();
	
	private Integer size;
	
	
	private String state = "ready";


	@NotNull
	@Transient
	private Integer stepUsers = 1;

	@NotNull
	@Transient
	private Integer stepVMs = 1;
	@Transient
	private String tabID; //for session navigation. See the controller doc for other information


	@Transient
	private  double thinkRate;     // 1/Z	

	@Transient
	private  Integer thinkTime;
	private String time;

	private int totalRuntime;
	
	private String instanceName;
	
	public SimulationsManager(){
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        java.util.Date date = new java.util.Date();
        this.date = dateFormat.format(date);
        this.time = timeFormat.format(date);
	}
	
	public void buildExperiments() {
		this.classList.clear();
		for (int numVMs = minNumVMs; numVMs <= maxNumVMs; numVMs = numVMs + stepVMs) 
			for (int numUsers = minNumUsers; numUsers <= maxNumUsers; numUsers = numUsers + stepUsers) 
				for (int it = 1; it <= this.numIter; it++) {
					InteractiveExperiment simClass = new InteractiveExperiment();
					simClass.setIter(it);
					simClass.setNumUsers(numUsers);
					simClass.setNumVMs(numVMs);
					simClass.setThinkTime(this.thinkTime);
					simClass.setInstanceName(this.instanceName);
					this.classList.add(simClass);
				}
	}
	
	
	public Integer getAccuracy() {
		return accuracy;
	}
	
	public List<InteractiveExperiment> getClassList() {
		return classList;
	}
	
	public String getDate() {
		return date;
	}
	
	public String getFolderPath() {
		return folderPath;
	}
	
	public Long getId() {
		return id;
	}
	
	
	public Solution getInputSolution() {
		return inputSolution;
	}

	public Integer getMaxNumUsers() {
		return maxNumUsers;
	}


	public Integer getMaxNumVMs() {
		return maxNumVMs;
	}

	public Integer getMinNumUsers() {
		return minNumUsers;
	}

	public Integer getMinNumVMs() {
		return minNumVMs;
	}
	
	public Integer getNumCompletedSimulations() {
		return numCompletedSimulations;
	}

	public Integer getNumIter() {
		return numIter;
	}

	public List<Simulation> getSimulationsList() {
		return simulationsList;
	}

	public int getSize() {
		this.size = classList.size();
		return size;
	}


	public String getState() {
		return state;
	}

	public Integer getStepUsers() {
		return stepUsers;
	}


	public Integer getStepVMs() {
		return stepVMs;
	}

	public String getTabID() {
		return tabID;
	}

	public double getThinkRate() {
		return thinkRate;
	}

	public Integer getThinkTime() {
		return thinkTime;
	}

	public String getTime() {
		return time;
	}

	public int getTotalRuntime() {
		return totalRuntime;
	}

	public void setAccuracy(Integer accuracy) {
		this.accuracy = accuracy;
	}

	public void setClassList(List<InteractiveExperiment> simulationsList) {
		this.classList = simulationsList;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setFolderPath(String folderPath) {
		this.folderPath = folderPath;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setInputSolution(Solution inputSolution) {
		this.inputSolution = inputSolution;
		Double tt= inputSolution.getLstSolutions().get(0).getJob().getThink();
		this.thinkTime = tt.intValue();
		this.instanceName = inputSolution.getId();

	}

	public void setMaxNumUsers(Integer maxNumUsers) {
		this.maxNumUsers = maxNumUsers;
	}

	public void setMaxNumVMs(Integer maxNumCores) {
		this.maxNumVMs = maxNumCores;
	}
	public void setMinNumUsers(Integer minUsers) {
		this.minNumUsers = minUsers;
	}
	public void setMinNumVMs(Integer minCores) {
		this.minNumVMs = minCores;
	}

	public void setNumCompletedSimulations(Integer num_of_completed_simulations) {
		this.numCompletedSimulations = num_of_completed_simulations;
	}

	public void setNumIter(Integer numIter) {
		this.numIter = numIter;
	}
	public void setSimulationsList(List<Simulation> simulationsList) {
		this.simulationsList = simulationsList;
	}

	public void setSize(Integer size){
		this.size = size;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setStepUsers(Integer stepUsrs) {
		this.stepUsers = stepUsrs;
	}

	public void setStepVMs(Integer stepVMs) {
		this.stepVMs = stepVMs;
	}

	public void setTabID(String tabID) {
		this.tabID = tabID;
	}

	public void setThinkRate(double thinkRate) {
		this.thinkRate = thinkRate;
	}

	public void setThinkTime(Integer thinkTime) {
		this.thinkTime = thinkTime;
	}

	public void setTime(String time) {
		this.time = time;
	}


	public void setTotalRuntime(int totalRuntime) {
		this.totalRuntime = totalRuntime;
	}
	
}
