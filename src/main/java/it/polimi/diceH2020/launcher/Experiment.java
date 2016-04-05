package it.polimi.diceH2020.launcher;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVMJobClassKey;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.model.SimulationsWIManager;
import it.polimi.diceH2020.launcher.service.DiceService;

@Scope("prototype")
@Component
public class Experiment {
	private String EVENT_ENDPOINT;
	private String INPUTDATA_ENDPOINT;
	private String RESULT_FOLDER;
	private String SOLUTION_ENDPOINT;
	private String STATE_ENDPOINT;
	private String SETTINGS_ENDPOINT;
	private String UPLOAD_ENDPOINT;

	private final Logger logger = Logger.getLogger(this.getClass().getName());
	private ObjectMapper mapper;
	private RestTemplate restTemplate = new RestTemplate();
	@Autowired
	private Settings settings;
	private boolean stop = false;
	private String port;
	
	@Autowired
	private DiceService ds;
	
	public Experiment(String port) {
		mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addKeyDeserializer(TypeVMJobClassKey.class, TypeVMJobClassKey.getDeserializer()); //setting KeyDeserializer for module, it's the API used for deserializing JSON
		mapper.registerModule(module);
		this.port = port;
	}

	public boolean isStop() {
		return stop;
	}

	public synchronized boolean initWI(InteractiveExperiment intExp){
		SimulationsWIManager simManager = (SimulationsWIManager)intExp.getSimulationsManager();
		Solution sol = simManager.getDecompressedInputJson();
		String solID = sol.getId();
		int jobID = sol.getSolutionPerJob(0).getJob().getId();
		String typeVMID = sol.getSolutionPerJob(0).getTypeVMselected().getId();
		String nameMapFile = String.format("%sMapJ%d%s.txt", solID, jobID, typeVMID);
		String nameRSFile = String.format("%sRSJ%d%s.txt", solID, jobID, typeVMID);
		try{
			send(nameMapFile, simManager.getDecompressedInputFile(0,2));
			send(nameRSFile, simManager.getDecompressedInputFile(0,3));
		}catch (Exception e){
			logger.info(e);
			return false;
		}
		it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings set = new it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings();
		set.setSimDuration(simManager.getSimDuration());
		set.setSolver(simManager.getSolver());
		set.setAccuracy(simManager.getAccuracy()/100.0);
		String res = restTemplate.postForObject(SETTINGS_ENDPOINT, set, String.class);
		logger.info(res);
		return true;
	}
	public synchronized boolean initOpt(InteractiveExperiment intExp){
		SimulationsManager simManager = intExp.getSimulationsManager();
		for(int i=0; i<simManager.getInputFiles().size();i++){
			String nameMapFile = simManager.getInputFiles().get(i)[0];
			String nameRSFile = simManager.getInputFiles().get(i)[1];
			try{
				send(nameMapFile, simManager.getDecompressedInputFile(i,2));
				send(nameRSFile, simManager.getDecompressedInputFile(i,3));
				System.out.println("sending:"+nameMapFile+", "+nameMapFile);
			}catch(Exception e){
				logger.debug("Error while sending replayer's files");
				return false;
			}
		}
		return true;
	}

	public synchronized boolean launchWI(InteractiveExperiment e) {
		
	  	e.setState(SimulationsStates.RUNNING);
	  	e.getSimulationsManager().refreshState();
		ds.updateManager(e.getSimulationsManager());
		//ds.updateExp(intExp); //TODO useful? @onetomany cascade.. 
		
		if (!initWI(e)||isStop()){
			restTemplate.postForObject(EVENT_ENDPOINT, Events.RESET, String.class);
			logger.info("[LOCKS] Exp"+e.getId()+" on port: "+port+" has been canceled"+"-> initialization of files");
			return false;
		}
		int num = e.getIter();
		String nameInstance = e.getInstanceName();
		String baseErrorString = "Iter: " + num + " Error for experiment: " + nameInstance;
		boolean idle = checkWSIdle();
		if (!idle || isStop()) {
			restTemplate.postForObject(EVENT_ENDPOINT, Events.RESET, String.class);
			logger.info("[LOCKS] Exp"+e.getId()+" on port: "+port+" has been canceled"+"-> service not idle");
			return false;
		}
		logger.info("[LOCKS] Exp"+e.getId()+"is been running on port:"+port);
		
		boolean charged_initsolution = sendSolution(e.getInputSolution());

		if (!charged_initsolution || isStop()) {
			logger.info(baseErrorString + "-> uploading the initial solution");
			restTemplate.postForObject(EVENT_ENDPOINT, Events.RESET, String.class);
			logger.info("[LOCKS] Exp"+e.getId()+" on port: "+port+" has been canceled"+ "-> uploading the initial solution");
			return false;
		}

		boolean evaluated_initsolution = evaluateInitSolution();
		if (!evaluated_initsolution || isStop()) {
			restTemplate.postForObject(EVENT_ENDPOINT, Events.RESET, String.class);
			logger.info("[LOCKS] Exp"+e.getId()+" on port: "+port+" has been canceled"+ "-> evaluating the initial solution");
			return false;
		}

		boolean update_experiment = updateExperiment(e);
		if (!update_experiment || isStop()) {
			restTemplate.postForObject(EVENT_ENDPOINT, Events.RESET, String.class);
			logger.info("[LOCKS] Exp"+e.getId()+" on port: "+port+" has been canceled"+ "-> updating the experiment information");
			return false;
		}
		// to go to idle
		restTemplate.postForObject(EVENT_ENDPOINT, Events.RESET, String.class);
		logger.info("[LOCKS] Exp"+e.getId()+" on port: "+port+" completed");
		return true;
	}

	private boolean updateExperiment(InteractiveExperiment e) {
		Solution sol = restTemplate.getForObject(SOLUTION_ENDPOINT, Solution.class);
		if (sol == null) return false;
		e.setResponseTime(sol.getSolutionPerJob(0).getDuration());
		e.setExperimentalDuration(sol.getOptimizationTime());
		e.setSol(sol);
		e.setDone(true);
		e.setNumSolutions(e.getNumSolutions()+1);
		return true;
	}

	private boolean evaluateInitSolution() {
		String res = restTemplate.postForObject(EVENT_ENDPOINT, Events.TO_EVALUATING_INIT, String.class);
		if (res.equals("EVALUATING_INIT")) {
			res = "EVALUATING_INIT"; //useful?
			while (res.equals("EVALUATING_INIT")) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				res = restTemplate.getForObject(STATE_ENDPOINT, String.class);
			}
			if (res.equals("EVALUATED_INITSOLUTION")) return true;
		}
		return false;
	}

	public synchronized boolean launchOpt(InteractiveExperiment e) {
		e.setState(SimulationsStates.RUNNING);
	  	e.getSimulationsManager().refreshState();
		ds.updateManager(e.getSimulationsManager());
		//ds.updateExp(intExp); //TODO useful? @onetomany cascade..
		if (!initOpt(e)||isStop()){
			restTemplate.postForObject(EVENT_ENDPOINT, Events.RESET, String.class);
			logger.info("[LOCKS] Exp"+e.getId()+" on port: "+port+" has been canceled"+"-> initialization of files");
			return false;
		}
		if (isStop()) return false;

		int num = e.getIter();
		//Path inputDataPath = e.getInstanceName();
		//if (!Files.exists(inputDataPath)) return;

		String nameInstance = e.getInstanceName();
		String baseErrorString = "Iter: " + num + " Error for experiment: " + nameInstance;

		boolean idle = checkWSIdle();

		if (!idle || isStop()) {
			logger.info(baseErrorString + "-> service not idle");
			return false;
		}

		boolean charged_inputdata = sendInputData(e.getInputData());

		if (!charged_inputdata || isStop()) return false;

		boolean charged_initsolution = generateInitialSolution();

		if (!charged_initsolution || isStop()) {
			logger.info(baseErrorString + "-> generation of the initial solution");
			return false;
		}

		boolean evaluated_initsolution = evaluateInitSolution();
		if (!evaluated_initsolution || isStop()) {
			logger.info(baseErrorString + "-> evaluating the initial solution");
			return false;
		}

		boolean initsolution_saved = saveInitSolution();
		if (!initsolution_saved) {
			logger.info(baseErrorString + "-> getting or saving initial solution");
			return false;
		}

		boolean finish = executeLocalSearch();

		if (!finish || isStop()) {
			logger.info(baseErrorString + "-> local search");
			return false;
		}

		boolean finalSolution_saved = saveFinalSolution(e);
		if (!finalSolution_saved) {
			logger.info(baseErrorString + "-> getting or saving final solution");
			return false;
		}
		// to go to idle
		restTemplate.postForObject(EVENT_ENDPOINT, Events.MIGRATE, String.class);

		//String percentage = BigDecimal.valueOf((double) analysisExecuted * 100 / (double) totalAnalysisToExecute).setScale(2, RoundingMode.HALF_EVEN).toString();
		
		logger.info("[LOCKS] Exp"+e.getId()+" on port: "+port+" completed");
		return true;
	}


	public void send(String filename, String content ) throws Exception{
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add("name", filename);
		map.add("filename", filename);
		try {
			ByteArrayResource contentsAsResource = new ByteArrayResource(content.getBytes("UTF-8"))  {
				@Override
				public String getFilename() {
					return filename;
				}
			};
			map.add("file", contentsAsResource);
			String res =  restTemplate.postForObject(UPLOAD_ENDPOINT, map, String.class);
			logger.info(res);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void send(Path f) {
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		String content;
		try {
			content = new String(Files.readAllBytes(f));

			final String filename = f.getFileName().toString();
			map.add("name", filename);
			map.add("filename", filename);
			ByteArrayResource contentsAsResource = new ByteArrayResource(content.getBytes("UTF-8")) {
				@Override
				public String getFilename() {
					return filename;
				}
			};
			map.add("file", contentsAsResource);
			restTemplate.postForObject(UPLOAD_ENDPOINT, map, String.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public boolean stop() {
		this.stop = true;
		String res = restTemplate.postForObject(EVENT_ENDPOINT, Events.RESET, String.class);
		if (res.equals("IDLE")) return true;
		else {
			logger.info(res);
			return false;
		}

	}

	public void waitForWS() {
		try {
			restTemplate.getForObject(STATE_ENDPOINT, String.class);
		} catch (Exception e) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			logger.info("trying to extablish a connection with S4C ws");
			waitForWS();
		}

	}

	private boolean checkWSIdle() {
		return checkWSIdle(0);

	}

	private boolean checkWSIdle(int iter) {
		if (iter > 50) { return false; }
		String res = restTemplate.getForObject(STATE_ENDPOINT, String.class);
		if (res.equals("IDLE")) return true;
		else {
			try {
				Thread.sleep(10000);
				return checkWSIdle(++iter);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return false;
			}
		}
	}

	private boolean executeLocalSearch() {
		String res = restTemplate.postForObject(EVENT_ENDPOINT, Events.TO_RUNNING_LS, String.class);
		if (res.equals("RUNNING_LS")) {
			res = "RUNNING_LS";
			while (res.equals("RUNNING_LS")) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				res = restTemplate.getForObject(STATE_ENDPOINT, String.class);
			}
		}
		if (res.equals("FINISH")) return true;
		else return false;
	}

	private boolean generateInitialSolution() {
		String res = restTemplate.postForObject(EVENT_ENDPOINT, Events.TO_RUNNING_INIT, String.class);
		if (res.equals("RUNNING_INIT")) {
			res = "RUNNING_INIT";
			while (res.equals("RUNNING_INIT")) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				res = restTemplate.getForObject(STATE_ENDPOINT, String.class);
			}
			if (res.equals("CHARGED_INITSOLUTION")) {
				return true;
			} else return false;
		} else return false;
	}

	@PostConstruct
	private void init() throws IOException {
		INPUTDATA_ENDPOINT = settings.getFullAddress() + port  + "/inputdata";
		EVENT_ENDPOINT = settings.getFullAddress() + port + "/event";
		STATE_ENDPOINT = settings.getFullAddress() + port + "/state";
		UPLOAD_ENDPOINT = settings.getFullAddress() + port + "/upload";
		SOLUTION_ENDPOINT = settings.getFullAddress() + port + "/solution";
		SETTINGS_ENDPOINT = settings.getFullAddress() + port +"/settings";
		Path result = Paths.get(settings.getResultDir());
		if (!Files.exists(result)) Files.createDirectory(result);
		RESULT_FOLDER = result.toAbsolutePath().toString();
	}

	private boolean saveFinalSolution(InteractiveExperiment e) {
		Solution sol = restTemplate.getForObject(SOLUTION_ENDPOINT, Solution.class);
		e.setSol(sol);
		e.setExperimentalDuration(sol.getOptimizationTime());
		e.setDone(true);
		e.setNumSolutions(e.getNumSolutions()+1);
		//e.setResponseTime(sol.getSolutionPerJob(0).getDuration());
		String msg = String.format("-%s iter: %d ->%s", e.getInstanceName(), e.getIter(), sol.toStringReduced());
		logger.info(msg);
		return true;
	}

	private boolean saveInitSolution() {
		Solution sol = restTemplate.getForObject(SOLUTION_ENDPOINT, Solution.class);
		String solFilePath = RESULT_FOLDER + File.separator + sol.getId() + "-MINLP.json";
		String solSerialized;
		try {
			solSerialized = mapper.writeValueAsString(sol);
			Files.write(Paths.get(solFilePath), solSerialized.getBytes());
			return true;
		} catch (JsonProcessingException e) {
			return false;
		}
		catch (IOException e) {
			return false;
		}
	}

	private boolean sendInputData(InstanceData data) {
		if (data != null) {
			String res = restTemplate.postForObject(INPUTDATA_ENDPOINT, data, String.class);
			if (res.equals("CHARGED_INPUTDATA")) return true;
			else {
				logger.info("Error for experiment: " + data.getId() + " server respondend in an unexpected way: " + res);
				return false;
			}
		} else {
			logger.info("Error in one experiment,  problem in inputdata serialization");
			return false;
		}
	}

	private boolean sendSolution(Solution solution) {
		String res = restTemplate.postForObject(SOLUTION_ENDPOINT, solution, String.class);
		if (res.equals("CHARGED_INITSOLUTION")) return true;
		else return false;
	}

	void wipeResultDir() throws IOException {
		Path result = Paths.get(settings.getResultDir());
		if (Files.exists(result)) {
			Files.walkFileTree(result, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}
			});
			Files.deleteIfExists(result);
			Files.createDirectory(result);
		}
	}

}
