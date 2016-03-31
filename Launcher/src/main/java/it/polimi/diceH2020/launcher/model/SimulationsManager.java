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

	private String folder = "";

	@Column(length = 20000000)
	private ArrayList<String[]> inputFiles = new ArrayList<String[]>();

	private String type = "";

	private String state = "ready";

	private String resultFilePath = "";

	@Column(length = 1000)
	private String input = new String();

	private String inputFileName = new String();

	@OneToMany(mappedBy = "simulationsManager", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@OrderColumn(name = "simManager_index")
	private List<InteractiveExperiment> experimentsList = new ArrayList<InteractiveExperiment>();

	private Integer numCompletedSimulations = 0;

	private Integer size = 0;

	public SimulationsManager() {
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

	public String getInputFile(Integer pos1, Integer pos2) {
		try {
			return Compressor.decompress(inputFiles.get(pos1)[pos2]);
		} catch (IOException e) {
			return "";
		}
	}

	public void addInputFiles(String mapName, String rsName, String mapContent, String rsContent) {
		String[] tmpList = new String[4];
		tmpList[0] = mapName;
		tmpList[1] = rsName;
		try {
			tmpList[2] = Compressor.compress(mapContent);
		} catch (IOException e) {
			e.printStackTrace();
			tmpList[2] = "";
		}
		try {
			tmpList[3] = Compressor.compress(rsContent);
		} catch (IOException e) {
			tmpList[3] = "";
		}
		this.inputFiles.add(tmpList);
	}

	public ArrayList<String[]> getInputFiles() {
		return inputFiles;
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
		if (experimentsList.isEmpty()) {
			this.size = 0;
		} else {
			this.size = experimentsList.size();
		}
	}

	public void writeFinalResults() {
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

	// public String getMapFileName() {
	// return mapFileName;
	// }
	//
	// public void setMapFileName(String mapFileName) {
	// this.mapFileName = mapFileName;
	// }
	//
	// public String getRsFileName() {
	// return rsFileName;
	// }
	//
	// public void setRsFileName(String rsFileName) {
	// this.rsFileName = rsFileName;
	// }

	public Long getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	// public boolean getMapFileEmpty(){
	// return mapFileEmpty;
	//
	// }
	// public boolean getRsFileEmpty(){
	// return rsFileEmpty;
	// }
	//
	// public void setMapFileEmpty(boolean mapFileEmpty) {
	// this.mapFileEmpty = mapFileEmpty;
	// }
	//
	// public void setRsFileEmpty(boolean rsFileEmpty) {
	// this.rsFileEmpty = rsFileEmpty;
	// }

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

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public String getInputFileName() {
		return inputFileName;
	}

	public void setInputFileName(String inputFileName) {
		this.inputFileName = inputFileName;
	}

	public void setInputFiles(ArrayList<String[]> inputFiles) {
		this.inputFiles = inputFiles;
	}
	
}
