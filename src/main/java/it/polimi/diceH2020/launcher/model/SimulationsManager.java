package it.polimi.diceH2020.launcher.model;

import it.polimi.diceH2020.launcher.SimulationsStates;
import it.polimi.diceH2020.launcher.utility.Compressor;
import lombok.Data;

import javax.persistence.*;
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

	@Column(length = 20000000) //...
	private ArrayList<String[]> inputFiles;

	private String type = "";

	private SimulationsStates state;

	private String resultFilePath;

	@Column(length = 1000)
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
		type = new String();
		folder = new String();
		
		state = SimulationsStates.READY;
	}

	public synchronized void refreshState(){
		int completed = 0;
		int error = 0;
		int running = 0;
		int expSize = experimentsList.size();
		for(int i=0; i<expSize;i++){
			if(experimentsList.get(i).getState().equals(SimulationsStates.COMPLETED)){
				completed++;
			}
			if(experimentsList.get(i).getState().equals(SimulationsStates.ERROR)){
				error++;
			}
			if(experimentsList.get(i).getState().equals(SimulationsStates.RUNNING)){
				running++;
			}
		}
		if(error > 0){
			if(running == 0){ // error+completed == expSize
				state = SimulationsStates.ERROR;
			}else{
				state = SimulationsStates.WARNING;
			}
		}else{
			if(running == 0){
				if(completed == expSize){// completed == expSize
					state = SimulationsStates.COMPLETED;
				}else{
					state = SimulationsStates.READY;
				}
			}else{
				state = SimulationsStates.RUNNING;
			}
		}
	}

	public String getInputFile(Integer pos1, Integer pos2) {
		try {
			return Compressor.originalDecompress(inputFiles.get(pos1)[pos2]);
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
}
