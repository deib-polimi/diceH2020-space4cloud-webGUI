package it.polimi.diceH2020.launcher.model;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;

import it.polimi.diceH2020.launcher.utility.Compressor;

@Entity
public class SimulationsManager {

	@Id
	@Column(name = "SIM_MANAGER")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	private String date;
	private String time;
	
	private String instanceName;
	
	@Transient
	private String mapFileName = "";
	@Transient
	private String rsFileName = "";
	
	@Column(length = 200000)
	private String mapFile;

	@Column(length = 200000)
	private String rsFile;
	
	private boolean mapFileEmpty = false;
	
	private boolean rsFileEmpty = false;
	
	private String type = "";
	
	private String state = "ready";
	
	private String resultFilePath = "";
	
	@Column(length = 1000)
	private String input = "";
	
	@OneToMany(mappedBy = "simulationsManager", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@OrderColumn(name = "simManager_index")
	private List<InteractiveExperiment> experimentsList = new ArrayList<InteractiveExperiment>();
	
	private Integer numCompletedSimulations = 0;
	
	private Integer size = 0;
	
	public SimulationsManager(){
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();
		this.date = dateFormat.format(date);
		this.time = timeFormat.format(date);
	}
	
	public String getDate() {
		return date;
	}

	public String getTime() {
		return time;
	}
	
	public Integer getNumCompletedSimulations() {
		return numCompletedSimulations;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public void setDate(String date) {
		this.date = date;
	}
	
	public void setTime(String time) {
		this.time = time;
	}
	
	public void setNumCompletedSimulations(Integer numCompletedSimulations) {
		this.numCompletedSimulations = numCompletedSimulations;
	}
	public String getMapFile() {
		try {
			return Compressor.decompress(mapFile);
		} catch (IOException e) {
			return "";
		}
	}

	public void setMapFile(String mapFile) {
		try {
			this.mapFile = Compressor.compress(mapFile);
		} catch (IOException e) {
			this.mapFile = "";
		}
	}

	public void setRsFile(String rsFile) {
		try {
			this.rsFile = Compressor.compress(rsFile);
		} catch (IOException e) {
			this.rsFile = "";
		}

	}

	public String getRsFile() {
		try {
			return Compressor.decompress(rsFile);
		} catch (IOException e) {
			return "";
		}
	}
	
	public List<InteractiveExperiment> getExperimentsList() {
		return experimentsList;
	}
	
	public void setExperimentList(List<InteractiveExperiment> simulationsList) {
		this.experimentsList = simulationsList;
	}
	
	public int getSize() {
		return size;
	}
	
	public void setSize() {
		if(experimentsList.isEmpty()){
			this.size = 0;
		}
		else{
			this.size = experimentsList.size();
		}
	}
	
	public void writeFinalResults(){
		;
	}
	
	public String getState() {
		return state;
	}
	
	public void setState(String state) {
		this.state = state;
	}
	
	public String getResultFilePath() {
		return resultFilePath;
	}

	public void setResultFilePath(String resultFilePath) {
		this.resultFilePath = resultFilePath;
	}

	public String getMapFileName() {
		return mapFileName;
	}

	public void setMapFileName(String mapFileName) {
		this.mapFileName = mapFileName;
	}

	public String getRsFileName() {
		return rsFileName;
	}

	public void setRsFileName(String rsFileName) {
		this.rsFileName = rsFileName;
	}

	public Long getId() {
		return id;
	}
	
	public String getType(){
		return type;
	}
	public void setType(String type){
		this.type = type;
	}
	
	public boolean getMapFileEmpty(){		
		return mapFileEmpty;
	
	}
	public boolean getRsFileEmpty(){
		return rsFileEmpty;
	}

	public void setMapFileEmpty(boolean mapFileEmpty) {
		this.mapFileEmpty = mapFileEmpty;
	}

	public void setRsFileEmpty(boolean rsFileEmpty) {
		this.rsFileEmpty = rsFileEmpty;
	}
	
	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}
	
	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}
}
