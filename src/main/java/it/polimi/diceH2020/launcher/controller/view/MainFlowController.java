/*
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

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.InteractiveExperimentRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.utility.SimulationsUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MainFlowController {

	@Autowired
	private DiceService ds;

	@Autowired
	private SimulationsManagerRepository simulationsManagerRepository;

	@Autowired
	private InteractiveExperimentRepository intExperimentRepository;

	@RequestMapping(value="/", method=RequestMethod.GET)
	public String showHome(SessionStatus sessionStatus, Model model){
		if(model.containsAttribute("sim_manager")){
			sessionStatus.isComplete();
		}
		model.addAttribute("wsStatusMap", ds.getWsStatus());
		model.addAttribute("queueSize", ds.getQueueSize());
		model.addAttribute("privateQueueSize", ds.getPrivateQueueSize());

		Map<Integer,Scenarios> scenarios = new HashMap<>();
		scenarios.put(0,Scenarios.PrivateAdmissionControl);
		scenarios.put(1,Scenarios.PrivateNoAdmissionControl);
		scenarios.put(2,Scenarios.PublicPeakWorkload);
		scenarios.put(3,Scenarios.PublicAvgWorkLoad);

		model.addAttribute("scenarios", scenarios);
		return "home";
	}

	//TODO deprecated
	@RequestMapping(value="/launch2", method=RequestMethod.GET)
	public String launchWithMultipleJson(@RequestParam("scenario") String scenario,SessionStatus sessionStatus, Model model){
		if(model.containsAttribute("sim_manager")){
			sessionStatus.isComplete();
		}
		model.addAttribute("scenario", Scenarios.valueOf(scenario));
		//model.addAttribute("activeJson", FileService.activeJson(Scenarios.valueOf(scenario)));

		return "launchSimulation_FileUploadWithMultipleJson";
	}


	@RequestMapping(value="/launch", method=RequestMethod.GET)
	public String launch(@RequestParam("scenario") String scenario, SessionStatus sessionStatus, Model model) {
		if(model.containsAttribute("sim_manager")){
			sessionStatus.isComplete();
		}

		model.addAttribute("scenario", Scenarios.valueOf(scenario));

		List<Scenarios> privateScenariosModels = new ArrayList<>();
		privateScenariosModels.add(Scenarios.PrivateAdmissionControl);
		privateScenariosModels.add(Scenarios.PrivateAdmissionControlWithPhysicalAssignment);
		model.addAttribute("Scenarios", privateScenariosModels);

		List<Scenarios> publicScenariosModels = new ArrayList<>();
		publicScenariosModels.add(Scenarios.PublicAvgWorkLoad);
		publicScenariosModels.add(Scenarios.StormPublicAvgWorkLoad);
		model.addAttribute("PublicScenarios", publicScenariosModels);

		return "launchSimulation_FileUpload";
	}

	@RequestMapping(value="/launchRetry", method=RequestMethod.GET)
	public String launch(@RequestParam("scenario") String scenario, @RequestParam("message") String message,SessionStatus sessionStatus, Model model){
		if(model.containsAttribute("sim_manager")){
			sessionStatus.isComplete();
		}
		List<Scenarios> privateScenariosModels = new ArrayList<Scenarios>();
		privateScenariosModels.add(Scenarios.PrivateAdmissionControl);
		privateScenariosModels.add(Scenarios.PrivateAdmissionControlWithPhysicalAssignment);
		model.addAttribute("scenario", Scenarios.valueOf(scenario));
		model.addAttribute("Scenarios",  privateScenariosModels);
		model.addAttribute("message", message);
		return "launchSimulation_FileUpload";
	}

	@RequestMapping(value="/resPub", method=RequestMethod.GET)
	public String listPub(Model model) {
		List<SimulationsManager> smList = simulationsManagerRepository
				.findByIdInOrderByIdAsc(simulationsManagerRepository
						.findPublicSimManGroupedByFolders(Scenarios.PublicAvgWorkLoad,
								Scenarios.PublicPeakWorkload, Scenarios.StormPublicAvgWorkLoad));
		model.addAttribute("folderList", getFolderList(smList));
		model.addAttribute("cloudType", "Public");
		return "resultsSimulations_GroupedByFolder";
	}

	@RequestMapping(value="/resPri", method=RequestMethod.GET)
	public String listPri(Model model){
		List<SimulationsManager> smList =simulationsManagerRepository.findByIdInOrderByIdAsc(simulationsManagerRepository.findPrivateSimManGroupedByFolders(Scenarios.PrivateAdmissionControl, Scenarios.PrivateNoAdmissionControl,Scenarios.PrivateAdmissionControlWithPhysicalAssignment));
		model.addAttribute("folderList", getFolderList(smList));
		model.addAttribute("cloudType", "Private");
		return "resultsSimulations_GroupedByFolder";
	}

	private List<Map<String,String>> getFolderList(List<SimulationsManager> smList){
		List<Map<String,String>> returnList = new ArrayList<>();

		for(SimulationsManager simMan : smList){

			Map<String,String> tmpMap = new HashMap<>();
			States state =  SimulationsUtilities.getStateFromList(simulationsManagerRepository.findStatesByFolder(simMan.getFolder()));

			tmpMap.put("date", simMan.getDate());
			tmpMap.put("time", simMan.getTime());
			tmpMap.put("scenario", simMan.getScenario().getAcronym());
			tmpMap.put("id", simMan.getId().toString());
			tmpMap.put("state", state.toString());
			tmpMap.put("input", simMan.getInput());
			tmpMap.put("folder",simMan.getFolder());
			tmpMap.put("num", String.valueOf(simulationsManagerRepository.countByFolder(simMan.getFolder())));
			tmpMap.put("completed", simMan.getNumCompletedSimulations().toString());
			returnList.add(tmpMap);
		}
		return returnList;
	}

	private List<Map<String,String>> getFolderContentList(List<SimulationsManager> smList){
		List<Map<String,String>> returnList = new ArrayList<>();

		for(SimulationsManager simMan : smList){
			for(InteractiveExperiment exp : simMan.getExperimentsList()){
				Map<String,String> tmpMap = new HashMap<>();

				tmpMap.put("date", simMan.getDate());
				tmpMap.put("time", simMan.getTime());
				tmpMap.put("scenario", simMan.getScenario().getDescription());
				tmpMap.put("id", simMan.getId().toString());
				tmpMap.put("input", simMan.getInput());
				tmpMap.put("folder",simMan.getFolder());
				//tmpMap.put("num", String.valueOf(simulationsManagerRepository.countByFolder(simMan.getFolder())));
				//			tmpMap.put("completed", simMan.getNumCompletedSimulations().toString());
				//
				//			tmpMap.put("size", String.valueOf(simMan.getSize()));
				tmpMap.put("state", simMan.getState().toString());
				tmpMap.put("provider", simMan.getProvider());
				tmpMap.put("cloudType", simMan.getScenario().getCloudType().toString());
				boolean error = false;
				try {
					for(SolutionPerJob spj: exp.getSol().getLstSolutions()){
						if(spj.getError()) error = true;
					}
				} catch (IOException e) {
					//TODO;
				}
				tmpMap.put("duration", String.valueOf(exp.getExperimentalDuration()));
				tmpMap.put("errors", String.valueOf(error));
				returnList.add(tmpMap);
			}
		}
		return returnList;
	}

	@RequestMapping(value="/resultsWI", method=RequestMethod.GET)
	public String resultsWI(@RequestParam("id") Long id,Model model,@ModelAttribute("message") String message) {
		SimulationsManager simManager = simulationsManagerRepository.findOne(id);
		model.addAttribute("sim", intExperimentRepository.findBySimulationsManagerOrderByIdAsc(simManager));
		return "simWIList";
	}

	@RequestMapping(value="/simOfFolder", method=RequestMethod.GET)
	public String simFolderContent(@RequestParam(value="folder") String folder, Model model) {
		model.addAttribute("simManagerList", getFolderContentList(simulationsManagerRepository.findByFolderOrderByIdAsc(folder)));
		return "resultsSimulations_inFolder";
	}

	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	public synchronized String deleteExperiment(@RequestParam(value="id") String folder,SessionStatus sessionStatus, Model model,HttpServletRequest request, RedirectAttributes redirectAttributes) {
		List<SimulationsManager> smList = simulationsManagerRepository.findByFolderOrderByIdAsc(folder);

		boolean invalidDeletion = invalidUpdate(smList);
		if(!invalidDeletion){
			simulationsManagerRepository.deleteByFolder(folder);
		}else{
			redirectAttributes.addFlashAttribute("message", "Cannot delete an uncompleted simulation.");
		}
		return "redirect:" + request.getHeader("Referer");
	}

	@RequestMapping(value = "/relaunch", method = RequestMethod.GET)
	public synchronized String relaunchExperiment (@RequestParam(value="id") String folder,SessionStatus sessionStatus, Model model,HttpServletRequest request, RedirectAttributes redirectAttributes) {
		List<SimulationsManager> smList = simulationsManagerRepository.findByFolderOrderByIdAsc(folder);
		boolean invalidDeletion = invalidUpdate(smList);
		if(!invalidDeletion){
			for (SimulationsManager sm : smList){
				for(InteractiveExperiment exp : sm.getExperimentsList()){
					exp.initializeAttributes();
					sm.refreshState();
					ds.updateManager(sm);
					ds.simulation(exp);
				}
			}
		}else{
			redirectAttributes.addFlashAttribute("message", "Cannot relaunch an uncompleted simulation.");
		}

		return "redirect:" + request.getHeader("Referer");
	}

	@RequestMapping(value = "/relaunchSelected", method = RequestMethod.GET)
	public synchronized String relaunchSelected(@RequestParam(value="ids[]") String ids,SessionStatus sessionStatus, Model model,HttpServletRequest request, RedirectAttributes redirectAttributes) {

		List<SimulationsManager> smList = new ArrayList<>();
		String[] parts = ids.split(",");
		for(String folderID : parts){
			smList.addAll(simulationsManagerRepository.findByFolderOrderByIdAsc(folderID));
		}

		boolean invalidDeletion = invalidUpdate(smList);
		if(!invalidDeletion){
			for (SimulationsManager sm : smList){
				for(InteractiveExperiment exp : sm.getExperimentsList()){
					exp.initializeAttributes();
					sm.refreshState();
					ds.updateManager(sm);
					ds.simulation(exp);
				}
			}
		}else{
			redirectAttributes.addFlashAttribute("message", "Cannot relaunch an uncompleted simulation.");
		}

		return "redirect:" + request.getHeader("Referer");
	}

	@RequestMapping(value = "/deleteSelected", method = RequestMethod.GET)
	public synchronized String deleteSelected(@RequestParam(value="ids[]") String ids,SessionStatus sessionStatus, Model model,HttpServletRequest request, RedirectAttributes redirectAttributes) {
		List<SimulationsManager> smList = new ArrayList<>();
		String[] parts = ids.split(",");
		for(String folderID : parts){
			smList.addAll(simulationsManagerRepository.findByFolderOrderByIdAsc(folderID));
		}

		boolean invalidDeletion = invalidUpdate(smList);
		if(!invalidDeletion){
			for(String folderID : parts){
				simulationsManagerRepository.deleteByFolder(folderID);
			}
		}else{
			redirectAttributes.addFlashAttribute("message", "Cannot delete an uncompleted simulation.");
		}
		return "redirect:" + request.getHeader("Referer");
	}

	private boolean invalidUpdate(List<SimulationsManager> smList){
		return smList.stream().anyMatch(s->s.getState().equals(States.RUNNING)||s.getState().equals(States.READY));
	}

}
