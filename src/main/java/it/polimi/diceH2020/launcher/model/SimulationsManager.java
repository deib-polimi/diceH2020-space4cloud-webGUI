package it.polimi.diceH2020.launcher.model;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.launcher.States;
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

	private States state;

	private String resultFilePath;

	private CloudType cloudType;

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

		experimentsList = new ArrayList<>();
		inputFiles = new ArrayList<>();

		inputFileName = "";
		input = "";
		resultFilePath = "";
		type = "";
		folder = "";

		cloudType = CloudType.Public;
		setState(States.READY);
	}

	public synchronized void refreshState(){
		int completed = 0;
		int error = 0;
		int running = 0;
		int expSize = experimentsList.size();
		for(int i=0; i<expSize;i++){
			if(experimentsList.get(i).getState().equals(States.COMPLETED)){
				completed++;
			}
			if(experimentsList.get(i).getState().equals(States.ERROR)){
				error++;
			}
			if(experimentsList.get(i).getState().equals(States.RUNNING)){
				running++;
			}
		}
		if(error > 0){
			if(running == 0){ // error+completed == expSize
				setState(States.ERROR);
			}else{
				setState(States.WARNING);
			}
		}else{
			if(running == 0){
				if(completed == expSize){// completed == expSize
					setState(States.COMPLETED);
				}else{
					setState(States.READY);
				}
			}else{
				setState(States.RUNNING);
			}
		}
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
}
