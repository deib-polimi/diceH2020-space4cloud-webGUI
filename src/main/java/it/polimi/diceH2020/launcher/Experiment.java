/*
Copyright 2017 Eugenio Gianniti
Copyright 2016 Jacopo Rigoli
Copyright 2016 Michele Ciavotta

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.launcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.service.DiceConsumer;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.service.RestCommunicationWrapper;
import it.polimi.diceH2020.launcher.utility.Compressor;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Scope("prototype")
@Component
public class Experiment {
	private final Logger logger = Logger.getLogger(getClass());

	private String EVENT_ENDPOINT;
	private String INPUT_DATA_ENDPOINT;
	private String RESULT_FOLDER;
	private String SOLUTION_ENDPOINT;
	private String STATE_ENDPOINT;
	private String UPLOAD_ENDPOINT;
	private String port;

	private DiceConsumer consumer;

	@Setter(onMethod = @__(@Autowired))
	private Settings settings;

	@Setter(onMethod = @__(@Autowired))
	private ObjectMapper mapper;

	@Setter(onMethod = @__(@Autowired))
	private DiceService diceService;

	@Setter(onMethod = @__(@Autowired))
	private RestCommunicationWrapper restWrapper;

	@Setter(onMethod = @__(@Autowired))
	private FileService fileService;

	public Experiment(DiceConsumer diceConsumer) {
		port = diceConsumer.getPort();
		consumer = diceConsumer;
	}

	private synchronized boolean initialize(InteractiveExperiment experiment) {
		SimulationsManager simManager = experiment.getSimulationsManager();
		boolean success = sendFiles (simManager.getInputFolders (), ".txt",
				String.format ("Impossible launching SimulationsManager %s: error with replayer files",
						simManager.getId()));
		if (success) {
			success = sendFiles (simManager.getInputFolders (), ".net",
					String.format ("Impossible launching SimulationsManager %s: error with .net file",
							simManager.getId()));
		}
		if (success) {
			success = sendFiles (simManager.getInputFolders (), ".def",
					String.format ("Impossible launching SimulationsManager %s: error with .def file",
							simManager.getId()));
		}
		return success;
	}

	private synchronized boolean sendFiles(@NotNull List<String> folders, @NotNull String extension,
										   @NotNull String errorMessage) {
		boolean success = true;
		try {
			Iterator<Map<String, String>> files = fileService.getFiles (folders, extension).iterator ();
			while (success && files.hasNext ()) {
				Map<String, String> fileInfo = files.next ();
				success = send(fileInfo.get ("name"), Compressor.decompress(fileInfo.get ("content")));
			}
		} catch (IOException e) {
			logger.error(errorMessage);
			success = false;
		}
		return success;
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

	public synchronized boolean launch(InteractiveExperiment e) {
		e.setState(States.RUNNING);
		e.getSimulationsManager().refreshState();
		diceService.updateManager(e.getSimulationsManager());

		String expInfo = String.format("|%s| ", Long.toString(e.getId()));
		String baseErrorString = expInfo+"Error! ";

		logger.info(String.format("%s-> {Exp:%s  port:%s,  provider:\"%s\" scenario:\"%s\"}",
				expInfo, Long.toString(e.getId()), port, e.getProvider(),
				e.getSimulationsManager().getScenario().toString()));
		logger.info(String.format("%s---------- Starting optimization ----------", expInfo));


		boolean idle = checkWSIdle();
		if (!idle) {
			logger.info(baseErrorString + " Service not idle");
			return false;
		}

		logger.info(String.format("%sAttempt to send .json",expInfo));
		boolean charged_inputdata = sendInputData(e.getInputData());
		if (!charged_inputdata) return false;
		logger.info(String.format("%s.json has been correctly sent",expInfo));


		logger.info(String.format("%sAttempt to send JMT replayers files",expInfo));

		if (!initialize(e)){
			logger.info(baseErrorString+" Problem with JMT replayers files");
			return false;
		}
		logger.info(expInfo+"JMT replayers files have been correctly sent");

		boolean charged_initsolution = generateInitialSolution();
		if (!charged_initsolution) {
			logger.info(baseErrorString + " Generation of the initial solution");
			return false;
		}
		logger.info(String.format("%s---------- Initial solution correctly generated",expInfo));

		boolean evaluated_initsolution = evaluateInitSolution();
		if (!evaluated_initsolution) {
			logger.info(baseErrorString + " Evaluating the initial solution");
			return false;
		}
		logger.info(String.format("%s---------- Initial solution correctly evaluated",expInfo));

		boolean initsolution_saved = saveInitSolution();
		if (!initsolution_saved) {
			logger.info(baseErrorString + " Getting or saving initial solution");
			return false;
		}
		logger.info(String.format("%s---------- Initial solution correctly saved",expInfo));


		logger.info(String.format("%s---------- Starting hill climbing", expInfo));
		boolean finish = executeLocalSearch();

		if (!finish) {
			logger.info(baseErrorString + " Local search");
			return false;
		}

		boolean finalSolution_saved = saveFinalSolution(e, expInfo);
		if (!finalSolution_saved) {
			logger.info(baseErrorString + " Getting or saving final solution");
			return false;
		}
		logger.info(String.format("%s---------- Finished hill climbing", expInfo));

		try{
			restWrapper.postForObject(EVENT_ENDPOINT, Events.MIGRATE, String.class); //from FINISHED to idle
		}catch(Exception exc){
			notifyWsUnreachability();
			return false;
		}
		logger.info(String.format("%s---------- Finished optimization ----------", expInfo));

		logger.debug("[LOCKS] Exp"+e.getId()+" on port: "+port+" completed");
		return true;
	}

	private boolean send(String filename, String content) {
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
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

			try {
				restWrapper.postForObject(UPLOAD_ENDPOINT, map, String.class);
			} catch (Exception e) {
				notifyWsUnreachability();
				return false;
			}
		} catch (UnsupportedEncodingException e) {
			logger.error (String.format ("error trying to send '%s'", filename), e);
			return false;
		}
		return true;
	}

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
		INPUT_DATA_ENDPOINT = settings.getFullAddress() + port  + "/inputdata";
		EVENT_ENDPOINT = settings.getFullAddress() + port + "/event";
		STATE_ENDPOINT = settings.getFullAddress() + port + "/state";
		UPLOAD_ENDPOINT = settings.getFullAddress() + port + "/upload";
		SOLUTION_ENDPOINT = settings.getFullAddress() + port + "/solution";
		Path result = Paths.get(settings.getResultDir());
		if (!Files.exists(result)) Files.createDirectory(result);
		RESULT_FOLDER = result.toAbsolutePath().toString();
	}

	private boolean saveFinalSolution(InteractiveExperiment e, String expInfo) {
		Solution sol;

		try{ sol = restWrapper.getForObject(SOLUTION_ENDPOINT, Solution.class); }
		catch(Exception exc){
			notifyWsUnreachability();
			return false;
		}

		e.setSol(sol);
		e.setExperimentalDuration(sol.getOptimizationTime());
		e.setDone(true);
		String msg = String.format("%sHill Climbing result  -> %s",expInfo, sol.toStringReduced());
		logger.info(msg);
		return true;
	}

	private boolean saveInitSolution() {
		Solution sol;
		try {
			sol = restWrapper.getForObject(SOLUTION_ENDPOINT, Solution.class);
		} catch (Exception e) {
			logger.info("Impossible receiving remote solution. ["+e+"]");
			notifyWsUnreachability();
			return false;
		}

		String solFilePath = RESULT_FOLDER + File.separator + sol.getId() + "-MINLP.json";
		String solSerialized;
		try {
			solSerialized = mapper.writeValueAsString(sol);
			Files.write(Paths.get(solFilePath), solSerialized.getBytes());
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private boolean sendInputData(InstanceDataMultiProvider data) {
		if (data != null) {
			String res;
			try{ res = restWrapper.postForObject(INPUT_DATA_ENDPOINT, data, String.class); }
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

	private void notifyWsUnreachability(){
		diceService.setChannelState(consumer,States.INTERRUPTED);
		logger.info("WS unreachable. (channel id: "+consumer.getId()+" port:"+consumer.getPort()+")");
	}

	private void notifyWsErrorState(String res){
		if(res.equals("ERROR")){
			diceService.setChannelState(consumer,States.ERROR);
			logger.info("WS is in error state. (channel id: "+consumer.getId()+" port:"+consumer.getPort()+")");
		}
	}
}
