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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVMJobClassKey;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.model.SimulationsWIManager;
import it.polimi.diceH2020.launcher.service.DiceConsumer;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.service.RestCommunicationWrapper;

@Scope("prototype")
@Component
public class Experiment {
	private String EVENT_ENDPOINT;
	private String INPUTDATA_ENDPOINT;
	private String RESULT_FOLDER;
	private String SOLUTION_ENDPOINT;
	private String STATE_ENDPOINT; //not used restTemplate.getForObject(STATE_ENDPOINT, String.class);
	private String SETTINGS_ENDPOINT;
	private String UPLOAD_ENDPOINT;

	private final Logger logger = Logger.getLogger(this.getClass().getName());
	private ObjectMapper mapper;
	@Autowired
	private Settings settings;
	private DiceConsumer consumer;
	
	@Autowired
	private DiceService ds;
	
	@Autowired
	private RestCommunicationWrapper restWrapper;
	
	public Experiment(DiceConsumer consumer) {
		mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addKeyDeserializer(TypeVMJobClassKey.class, TypeVMJobClassKey.getDeserializer()); //setting KeyDeserializer for module, it's the API used for deserializing JSON
		mapper.registerModule(module);
		this.consumer = consumer;
	}

	public synchronized boolean initWI(InteractiveExperiment intExp){
		SimulationsWIManager simManager = (SimulationsWIManager)intExp.getSimulationsManager();
		Solution sol = simManager.getDecompressedInputJson();
		String solID = sol.getId();
		int jobID = sol.getSolutionPerJob(0).getJob().getId();
		String typeVMID = sol.getSolutionPerJob(0).getTypeVMselected().getId();
		String nameMapFile = String.format("%sMapJ%d%s.txt", solID, jobID, typeVMID);
		String nameRSFile = String.format("%sRSJ%d%s.txt", solID, jobID, typeVMID);
		
		if(!send(nameMapFile, simManager.getDecompressedInputFile(0,2))){return false;}
		if(!send(nameRSFile, simManager.getDecompressedInputFile(0,3))){return false;}
		
		it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings set = new it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings();
		set.setSimDuration(simManager.getSimDuration());
		set.setSolver(simManager.getSolver());
		set.setAccuracy(simManager.getAccuracy()/100.0);
		String res;
		
		try{ res = restWrapper.postForObject(SETTINGS_ENDPOINT, set, String.class); }
		catch(Exception e){
			notifyWsUnreachability();
			return true;
		}
		
		logger.info(res);
		return true;
	}

	public synchronized boolean initOpt(InteractiveExperiment intExp){
		SimulationsManager simManager = intExp.getSimulationsManager();
		for(int i=0; i<simManager.getInputFiles().size();i++){
			String nameMapFile = simManager.getInputFiles().get(i)[0];
			String nameRSFile = simManager.getInputFiles().get(i)[1];
			if(!send(nameMapFile, simManager.getDecompressedInputFile(i,2)))return false;
			if(!send(nameRSFile, simManager.getDecompressedInputFile(i,3)))return false;
			System.out.println("sending:"+nameMapFile+", "+nameMapFile);
		}
		return true;
	}

	public synchronized boolean launchWI(InteractiveExperiment e) {
		
	  	e.setState(States.RUNNING);
	  	e.getSimulationsManager().refreshState();
		ds.updateManager(e.getSimulationsManager());
		//ds.updateExp(intExp); //TODO useful? @onetomany cascade.. 
		
		if (!initWI(e)){
			logger.info("[LOCKS] Exp"+e.getId()+" on port: "+consumer.getPort()+" has been canceled"+"-> initialization of files");
			return false;
		}
		boolean idle = checkWSIdle(); 
		if (!idle) {
			logger.info("[LOCKS] Exp"+e.getId()+" on port: "+consumer.getPort()+" has been canceled"+"-> service not idle");
			return false;
		}
		logger.info("[LOCKS] Exp"+e.getId()+"is been running on port:"+consumer.getPort());
		
		boolean charged_initsolution = sendSolution(e.getInputSolution());

		if (!charged_initsolution) {
			logger.info("[LOCKS] Exp"+e.getId()+" on port: "+consumer.getPort()+" has been canceled"+ "-> uploading the initial solution");
			return false;
		}

		boolean evaluated_initsolution = evaluateInitSolution();
		if (!evaluated_initsolution) {
			logger.info("[LOCKS] Exp"+e.getId()+" on port: "+consumer.getPort()+" has been canceled"+ "-> evaluating the initial solution");
			return false;
		}

		boolean update_experiment = updateExperiment(e);
		if (!update_experiment) {
			logger.info("[LOCKS] Exp"+e.getId()+" on port: "+consumer.getPort()+" has been canceled"+ "-> updating the experiment information");
			return false;
		}
		
		try{ 
			restWrapper.postForObject(EVENT_ENDPOINT, Events.RESET, String.class); //from evaluated_init to idle 
		}catch(Exception exc){
			notifyWsUnreachability();
			return false;
		}
		//TODO IF NOT IDLE...
		logger.info("[LOCKS] Exp"+e.getId()+" on port: "+consumer.getPort()+" completed");
		return true;
	}

	private boolean updateExperiment(InteractiveExperiment e) {
		Solution sol;
		
		try{ sol = restWrapper.getForObject(SOLUTION_ENDPOINT, Solution.class); }
		catch(Exception exc){
			notifyWsUnreachability();
			return false;
		}
		
		if (sol == null){
			return false;
		}
		e.setResponseTime(sol.getSolutionPerJob(0).getDuration());
		e.setExperimentalDuration(sol.getOptimizationTime());
		e.setSol(sol);
		e.setDone(true);
		e.setNumSolutions(e.getNumSolutions()+1);
		return true;
	}
	
	private boolean evaluateInitSolution() {
		String res;
		
		try{ res = restWrapper.postForObject(EVENT_ENDPOINT, Events.TO_EVALUATING_INIT, String.class); }
		catch(Exception e){
			notifyWsUnreachability();
			return false;
		}
		
		if (res.equals("EVALUATING_INIT")) {
			res = "EVALUATING_INIT"; 
			while (res.equals("EVALUATING_INIT")) {
				try { Thread.sleep(2000); }catch(InterruptedException e){e.printStackTrace();}
				
				try{ res = restWrapper.getForObject(STATE_ENDPOINT, String.class); }
				catch(Exception e){
					notifyWsUnreachability();
					return false;
				}
			}
		}
		if (res.equals("EVALUATED_INITSOLUTION")){
			return true;
		}
		notifyWsErrorState(res); 
		return false;
	}

	public synchronized boolean launchOpt(InteractiveExperiment e) {
		e.setState(States.RUNNING);
	  	e.getSimulationsManager().refreshState();
		ds.updateManager(e.getSimulationsManager());
		//ds.updateExp(intExp); //TODO useful? @onetomany cascade..
		if (!initOpt(e)){
			logger.info("[LOCKS] Exp"+e.getId()+" on port: "+consumer.getPort()+" has been canceled"+"-> initialization of files");
			return false;
		}
		int num = e.getIter();
		String nameInstance = e.getInstanceName();
		String baseErrorString = "Iter: " + num + " Error for experiment: " + nameInstance;

		boolean idle = checkWSIdle();

		if (!idle) {
			logger.info(baseErrorString + "-> service not idle");
			return false;
		}

		boolean charged_inputdata = sendInputData(e.getInputData());

		if (!charged_inputdata) return false;

		boolean charged_initsolution = generateInitialSolution();

		if (!charged_initsolution) {
			logger.info(baseErrorString + "-> generation of the initial solution");
			return false;
		}

		boolean evaluated_initsolution = evaluateInitSolution();
		if (!evaluated_initsolution) {
			logger.info(baseErrorString + "-> evaluating the initial solution");
			return false;
		}

		boolean initsolution_saved = saveInitSolution();
		if (!initsolution_saved) {
			logger.info(baseErrorString + "-> getting or saving initial solution");
			return false;
		}

		boolean finish = executeLocalSearch();

		if (!finish) {
			logger.info(baseErrorString + "-> local search");
			return false;
		}

		boolean finalSolution_saved = saveFinalSolution(e);
		if (!finalSolution_saved) {
			logger.info(baseErrorString + "-> getting or saving final solution");
			return false;
		}

		try{ 
			restWrapper.postForObject(EVENT_ENDPOINT, Events.MIGRATE, String.class); //from FINISHED to idle
		}catch(Exception exc){
			notifyWsUnreachability();
			return false;
		}
		logger.info("[LOCKS] Exp"+e.getId()+" on port: "+consumer.getPort()+" completed");
		return true;
	}

	public boolean send(String filename, String content ){
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
			
			try{ restWrapper.postForObject(UPLOAD_ENDPOINT, map, String.class); }
			catch(Exception e){
				notifyWsUnreachability();
				return false;
			}
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

//	public boolean send(Path f) {
//		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
//		String content;
//		try {
//			content = new String(Files.readAllBytes(f));
//
//			final String filename = f.getFileName().toString();
//			map.add("name", filename);
//			map.add("filename", filename);
//			ByteArrayResource contentsAsResource = new ByteArrayResource(content.getBytes("UTF-8")) {
//				@Override
//				public String getFilename() {
//					return filename;
//				}
//			};
//			map.add("file", contentsAsResource);
//			try{postObject(map);}catch(Exception e){return false;}
//		} catch (IOException e) {
//			e.printStackTrace();
//			return false;
//		}
//		return true;
//	}

//	public void waitForWS() {
//		try {
//			restTemplate.getForObject(STATE_ENDPOINT, String.class);
//		} catch (Exception e) {
//			try {
//				Thread.sleep(5000);
//			} catch (InterruptedException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//			logger.info("trying to extablish a connection with S4C ws");
//			waitForWS();
//		}
//
//	}

	/**
	 *  Wait until WS is in IDLE state.
	 *  It has a fixed max wait time.
	 *  Useful when WS has been stopped.
	 */
	private boolean checkWSIdle() {
		return checkWSIdle(50);  //quick fix(0-->50): WS cannot be stopped from launcher till now.
	}

	private boolean checkWSIdle(int iter) {
		if (iter > 50) { return false; }
		String res;
		
		try{ res = restWrapper.getForObject(STATE_ENDPOINT, String.class); }
		catch(Exception e){
			notifyWsUnreachability();
			return false;
		}
		
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
		String res;
		
		try{ res = restWrapper.postForObject(EVENT_ENDPOINT, Events.TO_RUNNING_LS, String.class); }
		catch(Exception e){
			notifyWsUnreachability();
			return false;
		}
		
		if (res.equals("RUNNING_LS")) {
			res = "RUNNING_LS";
			while (res.equals("RUNNING_LS")) {
				try{ Thread.sleep(2000); }catch(InterruptedException e){e.printStackTrace();}
				
				try{ res = restWrapper.getForObject(STATE_ENDPOINT, String.class); }
				catch(Exception e){
					notifyWsUnreachability();
					return false;
				}
			}
		}
		if (res.equals("FINISH")){
			return true;
		}
		notifyWsErrorState(res);
		return false;
	}

	private boolean generateInitialSolution() {
		String res;
		
		try{ res = restWrapper.postForObject(EVENT_ENDPOINT, Events.TO_RUNNING_INIT, String.class); }
		catch(Exception e){
			notifyWsUnreachability();
			return false;
		}
		
		if (res.equals("RUNNING_INIT")){
			res = "RUNNING_INIT";
			while (res.equals("RUNNING_INIT")){
				try{ Thread.sleep(2000); }catch(InterruptedException e){e.printStackTrace();}
				
				try{ res = restWrapper.getForObject(STATE_ENDPOINT, String.class); }
				catch(Exception e){
					notifyWsUnreachability();
					return false;
				}
			}
			if (res.equals("CHARGED_INITSOLUTION")){
				return true;
			}
		} 
		notifyWsErrorState(res);
		return false;
	}

	@PostConstruct
	private void init() throws IOException {
		INPUTDATA_ENDPOINT = settings.getFullAddress() + consumer.getPort()  + "/inputdata";
		EVENT_ENDPOINT = settings.getFullAddress() + consumer.getPort() + "/event";
		STATE_ENDPOINT = settings.getFullAddress() + consumer.getPort() + "/state";
		UPLOAD_ENDPOINT = settings.getFullAddress() + consumer.getPort() + "/upload";
		SOLUTION_ENDPOINT = settings.getFullAddress() + consumer.getPort() + "/solution";
		SETTINGS_ENDPOINT = settings.getFullAddress() + consumer.getPort() +"/settings";
		Path result = Paths.get(settings.getResultDir());
		if (!Files.exists(result)) Files.createDirectory(result);
		RESULT_FOLDER = result.toAbsolutePath().toString();
	}

	private boolean saveFinalSolution(InteractiveExperiment e) {
		Solution sol;
		
		try{ sol = restWrapper.getForObject(SOLUTION_ENDPOINT, Solution.class); }
		catch(Exception exc){
			notifyWsUnreachability();
			return false;
		}
		
		e.setSol(sol);
		e.setExperimentalDuration(sol.getOptimizationTime());
		e.setDone(true);
		e.setNumSolutions(e.getNumSolutions()+1);
		//e.setResponseTime(sol.getSolutionPerJob(0).getDuration());
		String msg = String.format("-%s iter: %d ->%s", e.getInstanceName(), e.getIter(), sol.toStringReduced());
		logger.info(msg);
		return true;
	}

	private boolean saveInitSolution() {//TODO usefull?
		
		Solution sol;
		
		try{ sol = restWrapper.getForObject(SOLUTION_ENDPOINT, Solution.class); }
		catch(Exception e){
			notifyWsUnreachability();
			return false;
		}
		
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
			String res;
			
			try{ res = restWrapper.postForObject(INPUTDATA_ENDPOINT, data, String.class); }
			catch(Exception e){ 
				notifyWsUnreachability();
				return false;
			}
			
			if (res.equals("CHARGED_INPUTDATA")) return true;
			else {
				logger.info("Error for experiment: " + data.getId() + " server respondend in an unexpected way: " + res);
				notifyWsErrorState(res);
				return false;
			}
		} else {
			logger.info("Error in one experiment,  problem in inputdata serialization");
			return false;
		}
	}
	
	private boolean sendSolution(Solution solution) {
		String res;
		
		try{ res = restWrapper.postForObject(SOLUTION_ENDPOINT, solution, String.class); }
		catch(Exception e){
			notifyWsUnreachability();
			return false;
		}
		
		if (res.equals("CHARGED_INITSOLUTION")){
			return true;
		}
		notifyWsErrorState(res);
		return false;
	}
	
	private void notifyWsUnreachability(){
		consumer.setState(States.INTERRUPTED);
		logger.info("WS unreachable. (channel id: "+consumer.getId()+" port:"+consumer.getPort()+")");
	}
	
	private void notifyWsErrorState(String res){
		if(res.equals("ERROR")){
			consumer.setState(States.ERROR);
			logger.info("WS is in error state. (channel id: "+consumer.getId()+" port:"+consumer.getPort()+")");
		}
	}
	
    //@Retryable(value = Exception.class,maxAttempts = 3)
	//private void takeBackToIdle(long id){
		//logger.info("Error with the WS");
	//	consumer.setWorking(false);
//try{
		 //	restTemplate.postForObject(EVENT_ENDPOINT, Events.RESET, String.class);
//		}catch(Exception exc){
//			logger.info("[LOCKS] Exp"+id+" on port: "+port+" has been canceled"+"-> communication with WS");
//		}
//	}
	
//	@Recover
//	private void recoverFromWsDisconnected(Exception exc){
//		logger.info("Error: communication with WS");
//		consumer.setWorking(false);
//		//TODO ds.refresh channels ... 
//	}

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
