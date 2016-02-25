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


/**
 * This class contain informations about client's requested set of simulations
 */
@Entity
public class SimulationsManager {
	
	@Id
	@Column(name = "SIM_MANAGER")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	private String date;
	private String time;
	private String model;
	@Transient
	private  double thinkRate;     // 1/Z	
	@Transient
	private  Integer thinkTime;         
	@Transient
	private  Integer minNumUsers;	
	@Transient
	private  Integer minNumVMs;	
	@Transient
	private  Integer maxNumUsers;	
	@Transient
	private  Integer maxNumVMs;
	private String folderPath;
	
	@Transient
	private Integer numIter;
	
	public Integer getNumIter() {
		return numIter;
	}


	public void setNumIter(Integer numIter) {
		this.numIter = numIter;
	}

	@NotNull
	@Transient
	private Integer stepVMs = 1;
	public Integer getStepVMs() {
		return stepVMs;
	}


	public void setStepVMs(Integer stepVMs) {
		this.stepVMs = stepVMs;
	}

	@NotNull
	@Transient
	private Integer stepUsers = 1;
	@NotNull
	private Integer accuracy = 5;

	@Transient
	private List<Simulations_class> classList = new ArrayList<Simulations_class>();
	
	@OneToMany(mappedBy = "simulationsManager", fetch = FetchType.LAZY,cascade=CascadeType.ALL)
	@OrderColumn(name = "simManager_index")
	private List<Simulation> simulationsList = new ArrayList<Simulation>();
	
	private int totalRuntime;
	private String state = "ready";
	
	@Transient
	private String tabID; //for session navigation. See the controller doc for other information
	
	private Integer numCompletedSimulations;
	
	public SimulationsManager(){
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        java.util.Date date = new java.util.Date();
        this.date = dateFormat.format(date);
        this.time = timeFormat.format(date);
	}
	
	
	public Integer getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(Integer accuracy) {
		this.accuracy = accuracy;
	}


	public Integer getStepUsers() {
		return stepUsers;
	}

	public void setStepUsers(Integer stepUsrs) {
		this.stepUsers = stepUsrs;
	}

	private int size=1;
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
	
	public Integer getNumCompletedSimulations() {
		return numCompletedSimulations;
	}

	public void setNumCompletedSimulations(Integer num_of_completed_simulations) {
		this.numCompletedSimulations = num_of_completed_simulations;
	}

	public void setClassList(List<Simulations_class> simulationsList) {
		this.classList = simulationsList;
	}

	public List<Simulations_class> getClassList() {
		return classList;
	}


	public double getThinkRate() {
		return thinkRate;
	}

	public void setThinkRate(double thinkRate) {
		this.thinkRate = thinkRate;
	}


	public Integer getThinkTime() {
		return thinkTime;
	}

	public void setThinkTime(Integer thinkTime) {
		this.thinkTime = thinkTime;
	}

	public Integer getMinNumUsers() {
		return minNumUsers;
	}

	public void setMinNumUsers(Integer minUsers) {
		this.minNumUsers = minUsers;
	}

	public Integer getMinNumVMs() {
		return minNumVMs;
	}

	public void setMinNumVMs(Integer minCores) {
		this.minNumVMs = minCores;
	}

	public Integer getMaxNumUsers() {
		return maxNumUsers;
	}

	public void setMaxNumUsers(Integer maxNumUsers) {
		this.maxNumUsers = maxNumUsers;
	}

	public Integer getMaxNumVMs() {
		return maxNumVMs;
	}

	public void setMaxNumVMs(Integer maxNumCores) {
		this.maxNumVMs = maxNumCores;
	}

	public int getTotalRuntime() {
		return totalRuntime;
	}

	public void setTotalRuntime(int totalRuntime) {
		this.totalRuntime = totalRuntime;
	}

	public String getTabID() {
		return tabID;
	}

	public void setTabID(String tabID) {
		this.tabID = tabID;
	}
	public void setSimulationsList(List<Simulation> simulationsList) {
		this.simulationsList = simulationsList;
	}
	public List<Simulation> getSimulationsList() {
		return simulationsList;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getFolderPath() {
		return folderPath;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setFolderPath(String folderPath) {
		this.folderPath = folderPath;
	}
	
}
