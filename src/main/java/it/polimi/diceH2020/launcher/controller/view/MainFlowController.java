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
package it.polimi.diceH2020.launcher.controller.view;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenario;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Technology;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.utility.SimulationsUtilities;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;

@Controller
public class MainFlowController {
	private final Logger logger = Logger.getLogger (getClass ());

	@Setter(onMethod = @__(@Autowired))
	private DiceService ds;

	@Setter(onMethod = @__(@Autowired))
	private SimulationsManagerRepository simulationsManagerRepository;

	@RequestMapping(value="/", method=RequestMethod.GET)
	public String showHome(SessionStatus sessionStatus, Model model){
		if(model.containsAttribute("sim_manager")){
			sessionStatus.isComplete();
		}
		model.addAttribute("wsStatusMap", ds.getWsStatus());
		model.addAttribute("queueSize", ds.getQueueSize());
		model.addAttribute("privateQueueSize", ds.getPrivateQueueSize());

		List<String> descriptions = new ArrayList<>();
		descriptions.add("Private cloud with Admission Control");
		descriptions.add("Private cloud without Admission Control");
		descriptions.add("Public cloud with LTC");
		descriptions.add("Public cloud without LTC");

		model.addAttribute("descriptions", descriptions);
		return "home";
	}

	@RequestMapping(value="/launch", method=RequestMethod.GET)
	public String launch(@RequestParam("cloudType") String cloudType, @RequestParam("longTermCommitment") String longTermCommitment, @RequestParam("admissionControl") String admissionControl, SessionStatus sessionStatus, Model model) {
		if(model.containsAttribute("sim_manager")){
			sessionStatus.isComplete();
		}

		model.addAttribute("cloudType", cloudType);
		model.addAttribute("longTermCommitment", longTermCommitment);
		model.addAttribute("admissionControl", admissionControl);

		List<Scenario> scenarios = new ArrayList<>();
		switch(CloudType.valueOf(cloudType)) {
			case PRIVATE:
				scenarios.add(new Scenario(Technology.SPARK, CloudType.PRIVATE, null, Boolean.valueOf(admissionControl)));
				scenarios.add(new Scenario(Technology.HADOOP, CloudType.PRIVATE, null, Boolean.valueOf(admissionControl)));
				break;
			case PUBLIC:
				scenarios.add(new Scenario(Technology.SPARK, CloudType.PUBLIC, Boolean.valueOf(longTermCommitment), null));
				scenarios.add(new Scenario(Technology.HADOOP, CloudType.PUBLIC, Boolean.valueOf(longTermCommitment), null));
				scenarios.add(new Scenario(Technology.STORM, CloudType.PUBLIC, Boolean.valueOf(longTermCommitment), null));
				break;
			default:
				throw new RuntimeException("Unknown type of cloud");
		}
		model.addAttribute("scenarios", scenarios);
		return "fileUpload";
	}

	@RequestMapping(value="/launchRetry", method=RequestMethod.GET)
	public String launch(@RequestParam("cloudType") String cloudType, @RequestParam("longTermCommitment") String longTermCommitment, @RequestParam("admissionControl") String admissionControl,  @RequestParam("message") String message,  SessionStatus sessionStatus, Model model) {
		if(model.containsAttribute("sim_manager")){
			sessionStatus.isComplete();
		}
		List<Scenario> privateScenariosModels = new ArrayList<>();
		privateScenariosModels.add(new Scenario(Technology.SPARK, CloudType.valueOf(cloudType), null, true));
		privateScenariosModels.add(new Scenario(Technology.SPARK, CloudType.valueOf(cloudType), null, true));
		model.addAttribute("scenario", new Scenario(Technology.SPARK, CloudType.valueOf(cloudType), null, Boolean.valueOf(admissionControl)));
		model.addAttribute("Scenarios",  privateScenariosModels);
		model.addAttribute("message", message);
		return "fileUpload";
	}

	@RequestMapping(value="/resPub", method=RequestMethod.GET)
	public String listPub(Model model) {
		List<SimulationsManager> smList = simulationsManagerRepository.findByIdInOrderByIdAsc(simulationsManagerRepository.findPublicSimManGroupedByFolders());
		model.addAttribute("folderList", getFolderList(smList));
		model.addAttribute("cloudType", "Public");
		return "simulationResults";
	}

	@RequestMapping(value="/resPri", method=RequestMethod.GET)
	public String listPri(Model model){
		List<SimulationsManager> smList = simulationsManagerRepository.findByIdInOrderByIdAsc(simulationsManagerRepository.findPrivateSimManGroupedByFolders());
		model.addAttribute("folderList", getFolderList(smList));
		model.addAttribute("cloudType", "Private");
		return "simulationResults";
	}

	private List<Map<String,String>> getFolderList(List<SimulationsManager> smList){
		List<Map<String,String>> returnList = new ArrayList<>();

		for(SimulationsManager simMan : smList){

			Map<String,String> tmpMap = new HashMap<>();
			States state = SimulationsUtilities.getStateFromList(
					simulationsManagerRepository.findStatesByFolder(simMan.getFolder()));

			tmpMap.put("date", simMan.getDate());
			tmpMap.put("time", simMan.getTime());
			tmpMap.put("scenario", simMan.getScenario().getShortDescription());
			tmpMap.put("id", simMan.getId().toString());
			tmpMap.put("state", state.toString());
			tmpMap.put("input", simMan.getInput());
			tmpMap.put("folder",simMan.getFolder());
			tmpMap.put("num", String.valueOf(simulationsManagerRepository.countByFolder(simMan.getFolder())));
			tmpMap.put("completed", simMan.getNumCompletedSimulations().toString());

			Optional<Double> result = Optional.empty ();
			// I expect this list to be always one element long
			for (InteractiveExperiment experiment: simMan.getExperimentsList ()) {
				logger.trace("Found experiment");
				if (experiment.isDone ()) {
					logger.trace("Experiment is done");
					try {
						Solution solution = experiment.getSol ();
						result = Optional.of (solution.getCost ());
					} catch (JsonParseException | JsonMappingException e) {
						logger.debug (String.format ("Error while parsing the solution JSON for experiment no. %d",
								experiment.getId ()), e);
					} catch (IOException e) {
						logger.debug (String.format ("Error while reading the solution for experiment no. %d",
								experiment.getId ()), e);
					}
				}
			}
			tmpMap.put ("result", result.map (Object::toString).orElse ("N/D"));

			returnList.add(tmpMap);
		}

		return returnList;
	}

	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	public synchronized String deleteExperiment(@RequestParam(value="id") String folder, HttpServletRequest request,
												RedirectAttributes redirectAttributes) {
		List<SimulationsManager> smList = simulationsManagerRepository.findByFolderOrderByIdAsc(folder);

		boolean invalidDeletion = invalidUpdate(smList);
		if (! invalidDeletion) {
			simulationsManagerRepository.deleteByFolder(folder);
		} else {
			redirectAttributes.addFlashAttribute("message", "Cannot delete an uncompleted simulation.");
		}
		return "redirect:" + request.getHeader("Referer");
	}

	@RequestMapping(value = "/relaunch", method = RequestMethod.GET)
	public synchronized String relaunchExperiment (@RequestParam(value="id") String folder, HttpServletRequest request,
												   RedirectAttributes redirectAttributes) {
		List<SimulationsManager> smList = simulationsManagerRepository.findByFolderOrderByIdAsc(folder);
		doRelaunch (smList, redirectAttributes);
		return "redirect:" + request.getHeader("Referer");
	}

	@RequestMapping(value = "/relaunchSelected", method = RequestMethod.GET)
	public synchronized String relaunchSelected(@RequestParam(value="folder[]") String[] folders,
												HttpServletRequest request, RedirectAttributes redirectAttributes) {

		List<SimulationsManager> smList = new ArrayList<>();

		for (String folderID : folders) {
			smList.addAll(simulationsManagerRepository.findByFolderOrderByIdAsc(folderID));
		}

		doRelaunch (smList, redirectAttributes);

		return "redirect:" + request.getHeader("Referer");
	}

	private synchronized void doRelaunch(@NotNull List<SimulationsManager> managers,
										 @NotNull RedirectAttributes redirectAttributes) {

		logger.trace("Relaunch");
		if (! invalidUpdate(managers)) {
			for (SimulationsManager sm : managers) {
				for (InteractiveExperiment exp : sm.getExperimentsList()) {
					exp.initializeAttributes();
					sm.refreshState();
					ds.updateManager(sm);
					ds.simulation(exp);
				}
			}
		} else {
			redirectAttributes.addFlashAttribute("message", "Cannot relaunch an incomplete simulation.");
		}
	}

	@RequestMapping(value = "/deleteSelected", method = RequestMethod.GET)
	public synchronized String deleteSelected(@RequestParam(value="folder[]") String[] folders,
											  HttpServletRequest request, RedirectAttributes redirectAttributes) {
		List<SimulationsManager> smList = new ArrayList<>();

		for (String folderID : folders) {
			smList.addAll(simulationsManagerRepository.findByFolderOrderByIdAsc(folderID));
		}

		boolean invalidDeletion = invalidUpdate(smList);
		if (!invalidDeletion) {
			for (String folderID : folders) {
				simulationsManagerRepository.deleteByFolder(folderID);
			}
		} else {
			redirectAttributes.addFlashAttribute("message", "Cannot delete an incomplete simulation.");
		}
		return "redirect:" + request.getHeader("Referer");
	}

	private boolean invalidUpdate(List<SimulationsManager> smList){
		return smList.stream().anyMatch(s->s.getState().equals(States.RUNNING)||s.getState().equals(States.READY));
	}

}
