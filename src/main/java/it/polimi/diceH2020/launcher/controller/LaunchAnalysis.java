/*
Copyright 2016 Jacopo Rigoli

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
package it.polimi.diceH2020.launcher.controller;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobProfile;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.service.Validator;
import it.polimi.diceH2020.launcher.utility.FileUtility;
import it.polimi.diceH2020.launcher.utility.JsonSplitter;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.naming.OperationNotSupportedException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.Random;

@SessionAttributes("sim_manager") // it will persist in each browser tab,
// resolved with
// http://stackoverflow.com/questions/368653/how-to-differ-sessions-in-browser-tabs/11783754#11783754
@Controller
@RequestMapping("/launch")
public class LaunchAnalysis {
	@Autowired
	Validator validator;

	@Autowired
	private DiceService ds;

	@Autowired
	FileUtility fileUtility;

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	@ModelAttribute("sim_manager")
	public SimulationsManager createSim_manager() {
		return new SimulationsManager();
	}

	@RequestMapping(value = "/error", method = RequestMethod.GET)
	public String showSimulationsManagerForm() {
		return "error";
	}

	@RequestMapping(value = "/simulationSetup", method = RequestMethod.GET)
	public String showSimulationsManagerForm(SessionStatus sessionStatus, Model model,
											 @ModelAttribute("instanceDataMultiProvider") String instanceDataMultiProviderPath,
											 @ModelAttribute("pathList") ArrayList<String> pathList,
											 @ModelAttribute("scenario") String scenarioString, RedirectAttributes redirectAttrs) {

		Scenarios scenario = Scenarios.valueOf(scenarioString);
		model.addAttribute("scenario", scenario);
		redirectAttrs.addAttribute("scenario", scenario);

		if(pathList.size() == 0){
			redirectAttrs.addAttribute("message", "You haven't submitted any file!");
			return "redirect:/launchRetry";
		}
		if (instanceDataMultiProviderPath == null) {
			redirectAttrs.addAttribute("message", "Select a Json file!");
			return "redirect:/launchRetry";
		}
		
		Optional<InstanceDataMultiProvider> idmp = validator.readInstanceDataMultiProvider(Paths.get(instanceDataMultiProviderPath));
		
		if(idmp.isPresent()){
			if(!idmp.get().validate()){
				model.addAttribute("message", idmp.get().getValidationError());
				return "redirect:/launchRetry";
			}
		}else{
			model.addAttribute("message", "Error with InstanceDataMultiProvider");
			return "redirect:/launchRetry";
		}

		InstanceDataMultiProvider instanceDataMultiProvider = idmp.get();

		String check = scenarioValidation(instanceDataMultiProvider, scenario);
		if(!check.equals("ok")) {
			redirectAttrs.addAttribute("message", check);
			return "redirect:/launchRetry";
		}

		List<InstanceDataMultiProvider> inputList = JsonSplitter.splitInstanceDataMultiProvider(instanceDataMultiProvider, scenario);
		List<SimulationsManager> simManagerList = initializeSimManagers(inputList);
		for(SimulationsManager sm : simManagerList){
			ArrayList<String> tmpList = new ArrayList<String>();
			pathList.forEach(e->{tmpList.add(e);});
			sm.setInputFileName(Paths.get(instanceDataMultiProviderPath).getFileName().toString());
			InstanceDataMultiProvider input = sm.getInputData();
			
			for (Entry<String, Map<String, Map<String, JobProfile>>> jobIDs : input.getMapJobProfiles().getMapJobProfile().entrySet()) {
				for (Entry<String, Map<String, JobProfile>> provider : jobIDs.getValue().entrySet()) {
					for (Entry<String, JobProfile> typeVMs : provider.getValue().entrySet()) {
						String mapFileName=new String();
						String rsFileName=new String();
						String mapFileContent=new String();
						String rsFileContent=new String();
		
						String fileNotFound = new String();
						try {
							mapFileName = getReplayersFileName("Map",input.getId(), jobIDs.getKey(), typeVMs.getKey());
							fileNotFound = mapFileName;
							File mapFile = fileUtility.provideFile(mapFileName);
							mapFileContent = new String(Files.readAllBytes(Paths.get(mapFile.getCanonicalPath())));
		
							rsFileName = getReplayersFileName("RS",input.getId(), jobIDs.getKey(), typeVMs.getKey());
							fileNotFound = rsFileName;
							File rsFile = fileUtility.provideFile(rsFileName);
							rsFileContent = new String(Files.readAllBytes(Paths.get(rsFile.getCanonicalPath())));
		
							if(mapFileContent.length()==0 || rsFileContent.length() == 0){
								throw new OperationNotSupportedException("One or more replayers submitted files are empty");
							}
							sm.addInputFiles(mapFileName,rsFileName,mapFileContent,rsFileContent);
							sm.setNumCompletedSimulations(0);
							sm.buildExperiments();
						} catch (IOException e1) {
							logger.info("File \""+fileNotFound+"\" is missing.");
							deleteUploadedFiles(pathList);
							redirectAttrs.addAttribute("message","File \""+fileNotFound+"\" is missing.");
							return "redirect:/launchRetry";
						} catch (OperationNotSupportedException e1) {
							logger.info(e1.getMessage());
							deleteUploadedFiles(pathList);
							redirectAttrs.addAttribute("message", e1.getMessage());
							return "redirect:/launchRetry";
						}
					}
				}
			}
		}
		deleteUploadedFiles(pathList);

		for(SimulationsManager sm : simManagerList){
			ds.simulation(sm);
		}
		model.addAttribute("simManagersList",simManagerList);
		return "redirect:/";
	}

	@RequestMapping(value = "/simulationSetupSingleInputData", method = RequestMethod.GET)
	public String showSimulationsManagerFormSingleData(SessionStatus sessionStatus, Model model,
													   @ModelAttribute("instanceData") String instanceDataPath,
													   @ModelAttribute("pathList") ArrayList<String> pathList,
													   @ModelAttribute("scenario") String scenarioString, RedirectAttributes redirectAttrs) {

		Scenarios scenario = Scenarios.valueOf(scenarioString);
		model.addAttribute("scenario", scenario);
		redirectAttrs.addAttribute("scenario", scenario);
		if (instanceDataPath == null) {
			redirectAttrs.addAttribute("message", "Select a Json file!");
			return "redirect:/launchRetry";
		}

		InstanceDataMultiProvider instanceData = validator.objectFromPath(Paths.get(instanceDataPath), InstanceDataMultiProvider.class).get();

		String check = scenarioValidation(instanceData, scenario);
		if(!check.equals("ok")) {
			redirectAttrs.addAttribute("message", check);
			return "redirect:/launchRetry";
		}

		List<InstanceDataMultiProvider> inputList = new ArrayList<>();
		inputList.add(instanceData);
		List<SimulationsManager> simManagerList = initializeSimManagers(inputList);

		ArrayList<String> tmpList = new ArrayList<String>();
		pathList.forEach(e->{tmpList.add(e);});
		for(SimulationsManager sm : simManagerList){
			sm.setInputFileName(Paths.get(instanceDataPath).getFileName().toString());

			InstanceDataMultiProvider input = sm.getInputData();
			for (Entry<String, Map<String, Map<String, JobProfile>>> jobIDs : input.getMapJobProfiles().getMapJobProfile().entrySet()) {
				for (Entry<String, Map<String, JobProfile>> provider : jobIDs.getValue().entrySet()) {
					for (Entry<String, JobProfile> typeVMs : provider.getValue().entrySet()) {
			
						String mapFileName=new String();
						String rsFileName=new String();
						String mapFileContent=new String();
						String rsFileContent=new String();
						String fileNotFound= new String();
						try {
							mapFileName = getReplayersFileName("Map",input.getId(), jobIDs.getKey(), typeVMs.getKey());
							fileNotFound = mapFileName;
							File mapFile = fileUtility.provideFile(mapFileName);
							mapFileContent = new String(Files.readAllBytes(Paths.get(mapFile.getCanonicalPath())));
		
							rsFileName = getReplayersFileName("RS",input.getId(), jobIDs.getKey(), typeVMs.getKey());
							fileNotFound = rsFileName;
							File rsFile = fileUtility.provideFile(rsFileName);
							rsFileContent = new String(Files.readAllBytes(Paths.get(rsFile.getCanonicalPath())));
		
							if(mapFileContent.length()==0 || rsFileContent.length() == 0){
								throw new OperationNotSupportedException("One or more replayers submitted files are empty");
							}
							sm.addInputFiles(mapFileName,rsFileName,mapFileContent,rsFileContent);
							sm.setNumCompletedSimulations(0);
							sm.buildExperiments();
		
						} catch (IOException e1) {
							logger.info("File \""+fileNotFound+"\" is missing.");
							deleteUploadedFiles(pathList);
							redirectAttrs.addAttribute("message", "File \""+fileNotFound+"\" is missing.");
							return "redirect:/launchRetry";
						} catch (OperationNotSupportedException e1) {
							logger.info(e1.getMessage());
							deleteUploadedFiles(pathList);
							redirectAttrs.addAttribute("message", e1.getMessage());
							return "redirect:/launchRetry";
						}
					}
				}
			}
		}

		deleteUploadedFiles(pathList);

		for(SimulationsManager sm : simManagerList){
			ds.simulation(sm);
		}
		model.addAttribute("simManagersList",simManagerList);
		return "redirect:/";
	}

	private String getReplayersFileName(String typeOfFile,String idA, String vmType, String idC ){ //TODO move to shared
		return idA+typeOfFile+"J"+vmType + idC + ".txt";
	}


	private String scenarioValidation(InstanceDataMultiProvider instanceDataMultiProvider, Scenarios scenario){
		String returnString = new String();
		if(instanceDataMultiProvider.getMapJobProfiles()==null || instanceDataMultiProvider.getMapClassParameters()==null){
			returnString = "Json is missing some required parameters(MapJobProfiles or MapClassParameters)!";
			return returnString;
		}

		switch (scenario) {
			case PrivateAdmissionControl:
				if(instanceDataMultiProvider.getPrivateCloudParameters()==null||instanceDataMultiProvider.getMapVMConfigurations()==null){
					returnString = "Json is missing some required parameters(PrivateCloudParameters or MapVMConfigurations)!";
					return returnString;
				}
				if (!instanceDataMultiProvider.getPrivateCloudParameters().validate()) {
					returnString = "Private Cloud Parameters uploaded in Json aren't valid!";
					return returnString;
				}
				if (!instanceDataMultiProvider.getMapVMConfigurations().validate()) {
					returnString = "VM Configurations uploaded in Json aren't valid!";
					return returnString;
				}
				if(instanceDataMultiProvider.getProvidersList().size() != 1){
					returnString = "A private scenario cannot have multiple providers!(call you providers:\"inHouse\")";
					return returnString;
				}
				break;

			case PrivateNoAdmissionControl:
				if(instanceDataMultiProvider.getMapVMConfigurations()==null){
					returnString = "Json is missing some required parameters(MapVMConfigurations)!";
					return returnString;
				}
				if(instanceDataMultiProvider.getMapVMConfigurations().getMapVMConfigurations()==null){
					returnString = "Json is missing some required parameters(MapVMConfigurations)!";
					return returnString;
				}
				if (!instanceDataMultiProvider.getMapVMConfigurations().validate()) {
					returnString = "VM Configurations uploaded in Json aren't valid!";
					return returnString;
				}
				if(instanceDataMultiProvider.getProvidersList().size() != 1){
					returnString = "A private scenario cannot have multiple providers!(call you providers:\"inHouse\")";
					return returnString;
				}
				break;

			case PublicPeakWorkload:
				if(instanceDataMultiProvider.getMapPublicCloudParameters()==null || instanceDataMultiProvider.getMapPublicCloudParameters().getMapPublicCloudParameters()==null){
					returnString = "Json is missing some required parameters(MapPublicCloudParameters)!";
					return returnString;
				}
				if (!instanceDataMultiProvider.getMapPublicCloudParameters().validate()) {
					returnString = "Public Cloud Parameters uploaded in Json aren't valid!";
					return returnString;
				}
				break;

			case PublicAvgWorkLoad:
				break;

			default:
				new Exception("Error with scenario files");
				break;
		}
		return "ok";
	}

	private List<SimulationsManager> initializeSimManagers(List<InstanceDataMultiProvider> inputList){
		List<SimulationsManager> simManagerList = new ArrayList<SimulationsManager>();
		String folder = generateUniqueString();

		for(InstanceDataMultiProvider instanceData : inputList){
			SimulationsManager simManager = new SimulationsManager();
			simManager.setInputData(instanceData);
			simManager.setScenario(instanceData.getScenario().get());
			simManager.setFolder(folder);

			simManagerList.add(simManager);
		}
		return simManagerList;
	}

	/**
	 * Round doubles without losing precision.
	 *
	 * @param unrounded
	 * @param precision
	 * @param roundingMode
	 * @return
	 */
	public static double round(double unrounded, int precision, int roundingMode) {
		BigDecimal bd = new BigDecimal(unrounded);
		BigDecimal rounded = bd.setScale(precision, roundingMode);
		return rounded.doubleValue();
	}

	private void deleteUploadedFiles(List<String> pathList){
		List<File> filesToBeEreased = new ArrayList<>();
		for(String  path :pathList){
			try {
				filesToBeEreased.add(fileUtility.provideFile(path));
			} catch (IOException e) {
				continue;
			}
		}
		fileUtility.delete(filesToBeEreased);
	}

	private String generateUniqueString() {
		//String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		Date dNow = new Date( );
		SimpleDateFormat ft = new SimpleDateFormat ("Edd-MM-yyyy_HH-mm-ss");
		Random random = new Random();
		String id = ft.format(dNow)+random.nextInt(99999);
		return id;
	}
}
