package it.polimi.diceH2020.launcher.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
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


/**
 * This class contain informations about client's requested set of simulations
 * In case of V7 simulations is used also to initialize multiple classes (useful for having the form in sequential page)
 */
@Entity
public class SimulationsManager {
	
	@Id
	@Column(name = "SIM_MANAGER")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	/*
	 * COMMON ATTRIBUTES
	 * If these attributes are initialized all the simulations have these same values.
	 */
	private String date;
	private String time;
	private String model;
	
	@Transient
	private  Integer map;              //number of Maps
	@Transient
	private  Integer reduce;           //number of Reduce
	@Transient
	private  double mapRate;       // 1/(Avg duration of a map)
	@Transient
	private  double reduceRate;    // 1/(Avg duration of a reduce)
	@Transient
	private  double thinkRate;     // 1/Z
	
	@Transient
	private  Integer mapTime;           // 1/(Avg duration of a map)
	@Transient
	private  Integer reduceTime;        // 1/(Avg duration of a reduce)
	@Transient
	private  Integer thinkTime;          // 1/Z
	
	@Transient
	private  Integer minNumUsers;	
	@Transient
	private  Integer minNumCores;	
	@Transient
	private  Integer maxNumUsers;	
	@Transient
	private  Integer maxNumCores;
	private String folderPath;
	
	@NotNull
	@Transient
	private Integer stepCores = 1;
	@NotNull
	@Transient
	private Integer stepUsrs = 1;
	@NotNull
	private boolean erlang = true;
	@NotNull
	private Integer accuracy = 5;
	/*@NotNull
	private Integer confidenceInterval; //hardcoded*/

	private Integer minNumOfBatch;    
	private Integer maxNumOfBatch;

	@Transient
	private List<Simulations_class> classList = new ArrayList<Simulations_class>();
	
	/*@JoinColumn(name = "SIM_MANAGER")
	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY)//(mappedBy = "id", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	*/
	//@OneToMany(mappedBy = "id", cascade = CascadeType.ALL)
	@OneToMany(mappedBy = "simulationsManager", fetch = FetchType.LAZY,cascade=CascadeType.ALL)
	@OrderColumn(name = "simManager_index")
	private List<Simulation> simulationsList = new ArrayList<Simulation>();
	
	private int totalRuntime;
	private String state = "ready";
	
	@Transient
	private String tabID; //for session navigation. See the controller doc for other information
	
	/*
	 * COMMON ATTRIBUTES
	 */
	
	@NotNull
	@Min(1)
	@Max(2)
	@Transient
	private Integer class_number; 	
	
	
	private Integer num_of_completed_simulations;
	
	public SimulationsManager(){
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        java.util.Date date = new java.util.Date();
        this.date = dateFormat.format(date);
        this.time = timeFormat.format(date);
	}
	
	public Integer getMinNumOfBatch() {
		return minNumOfBatch;
	}

	public void setMinNumOfBatch(Integer minNumOfBatch) {
		this.minNumOfBatch = minNumOfBatch;
	}

	public Integer getMaxNumOfBatch() {
		return maxNumOfBatch;
	}

	public void setMaxNumOfBatch(Integer maxNumOfBatch) {
		this.maxNumOfBatch = maxNumOfBatch;
	}
	
	public Integer getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(Integer accuracy) {
		this.accuracy = accuracy;
	}

	public boolean isErlang() {
		return erlang;
	}

	public void setErlang(boolean erlang) {
		this.erlang = erlang;
	}

	public Integer getStepCores() {
		return stepCores;
	}

	public void setStepCores(Integer stepCores) {
		this.stepCores = stepCores;
	}

	public Integer getStepUsrs() {
		return stepUsrs;
	}

	public void setStepUsrs(Integer stepUsrs) {
		this.stepUsrs = stepUsrs;
	}

	private int size=1;
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
	
	public Integer getNum_of_completed_simulations() {
		return num_of_completed_simulations;
	}

	public void setNum_of_completed_simulations(Integer num_of_completed_simulations) {
		this.num_of_completed_simulations = num_of_completed_simulations;
	}

	public void setClassList(List<Simulations_class> simulationsList) {
		this.classList = simulationsList;
	}

	public List<Simulations_class> getClassList() {
		return classList;
	}

	public Integer getClass_number() {
		return class_number;
	}

	public void setClass_number(Integer class_number) {
		this.class_number = class_number;
	}

	public Integer getMap() {
		return map;
	}

	public void setMap(Integer map) {
		this.map = map;
	}

	public Integer getReduce() {
		return reduce;
	}

	public void setReduce(Integer reduce) {
		this.reduce = reduce;
	}

	public double getMapRate() {
		return mapRate;
	}

	public void setMapRate(double mapRate) {
		this.mapRate = mapRate;
	}

	public double getReduceRate() {
		return reduceRate;
	}

	public void setReduceRate(double reduceRate) {
		this.reduceRate = reduceRate;
	}

	public double getThinkRate() {
		return thinkRate;
	}

	public void setThinkRate(double thinkRate) {
		this.thinkRate = thinkRate;
	}

	public Integer getMapTime() {
		return mapTime;
	}

	public void setMapTime(Integer mapTime) {
		this.mapTime = mapTime;
	}

	public Integer getReduceTime() {
		return reduceTime;
	}

	public void setReduceTime(Integer reduceTime) {
		this.reduceTime = reduceTime;
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

	public Integer getMinNumCores() {
		return minNumCores;
	}

	public void setMinNumCores(Integer minCores) {
		this.minNumCores = minCores;
	}

	public Integer getMaxNumUsers() {
		return maxNumUsers;
	}

	public void setMaxNumUsers(Integer maxNumUsers) {
		this.maxNumUsers = maxNumUsers;
	}

	public Integer getMaxNumCores() {
		return maxNumCores;
	}

	public void setMaxNumCores(Integer maxNumCores) {
		this.maxNumCores = maxNumCores;
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
	

	/*
	public Integer getConfidenceInterval() {
		return confidenceInterval;
	}

	public void setConfidenceInterval(Integer confidenceInterval) {
		this.confidenceInterval = confidenceInterval;
	}*/
}
