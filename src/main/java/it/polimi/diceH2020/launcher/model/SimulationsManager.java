package it.polimi.diceH2020.launcher.model;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.utility.Compressor;
import it.polimi.diceH2020.launcher.utility.SimulationsUtilities;
import lombok.Data;

import javax.persistence.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
public class SimulationsManager {

	@Id
	@Column(name = "SIM_MANAGER")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String date;
	
	private String time;

	private String instanceName;

	private String folder;
	
	private String provider;
	
	private Scenarios scenario; //	contains also CloudType cloudType;

	@Column(length = 20000000) //...
	private ArrayList<String[]> inputFiles;

	private States state;

	private String resultFilePath;
	
	@Transient
	private InstanceData inputData;
	
//	@Transient
//	private String tabID; //for session navigation. See the controller doc for other information

	@Column(length = 10000)
	private String input;

	private String inputFileName;

	@OneToMany(mappedBy = "simulationsManager", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderColumn(name = "simManager_index")
	private List<InteractiveExperiment> experimentsList;

	private Integer numCompletedSimulations;
	
	private Integer numFailedSimulations;

	public SimulationsManager() {
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();
		this.date = dateFormat.format(date);
		this.time = timeFormat.format(date);
		numFailedSimulations = 0;
		numCompletedSimulations = 0;
		
		experimentsList = new ArrayList<InteractiveExperiment>();
		inputFiles = new ArrayList<String[]>();
		
		inputFileName = new String();
		input = new String();
		resultFilePath = new String();
		folder = new String();

		state = States.READY;
	}
	
	public synchronized void refreshState(){
		
		List<States> statesList = new ArrayList<>();
		for(InteractiveExperiment intExp : experimentsList){
			statesList.add(intExp.getState());
		}
		state = SimulationsUtilities.getStateFromList(statesList);
	}

	public String getDecompressedInputFile(Integer pos1, Integer pos2) {
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

	public int getSize(){
		if (experimentsList.isEmpty()) {
			return 0;
		} else {
			return experimentsList.size();
		}
	}

	public void writeFinalResults() {
		;
	}

	public void buildExperiments() {
		experimentsList.clear();
		InteractiveExperiment experiment = new InteractiveExperiment(getInstanceName(), this.provider, this);
		//System.out.println(getInstanceName());
		experimentsList.add(experiment);
	}

	public void setInputData(InstanceData inputData) {
		this.inputData = inputData;

		try {
			ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module());
			setInput(Compressor.compress(mapper.writeValueAsString(inputData)));
		} catch (IOException e) {
			setInput("Error");
		}
		setInstanceName(inputData.getId());
		//System.out.println("id:"+inputData.getId());
	}

	public InstanceData getDecompressedInputData() {
		if (inputData != null) {
			return inputData;
		} else if (getInput() != null) {
			try {
				ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module());
				return getInput().equals("") || getInput().equals("Error") ? null : mapper.readValue(Compressor.decompress(getInput()), InstanceData.class);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return inputData;
	}
}