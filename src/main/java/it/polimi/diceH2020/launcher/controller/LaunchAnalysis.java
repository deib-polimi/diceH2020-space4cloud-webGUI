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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
			deleteUploadedFiles(pathList);
			redirectAttrs.addAttribute("message", "You haven't submitted any file!");
			return "redirect:/launchRetry";
		}
		if (instanceDataMultiProviderPath == null) {
			deleteUploadedFiles(pathList);
			redirectAttrs.addAttribute("message", "Select a Json file!");
			return "redirect:/launchRetry";
		}
		
		Optional<InstanceDataMultiProvider> idmp = validator.readInstanceDataMultiProvider(Paths.get(instanceDataMultiProviderPath));
		
		if(idmp.isPresent()){
			if(!idmp.get().validate()){
				deleteUploadedFiles(pathList);
				model.addAttribute("message", idmp.get().getValidationError());
				return "redirect:/launchRetry";
			}
		}else{
			model.addAttribute("message", "Error with InstanceDataMultiProvider");
			deleteUploadedFiles(pathList);
			return "redirect:/launchRetry";
		}

		InstanceDataMultiProvider instanceDataMultiProvider = idmp.get();

		String check = scenarioValidation(instanceDataMultiProvider, scenario);
		if(!check.equals("ok")) {
			deleteUploadedFiles(pathList);
			redirectAttrs.addAttribute("message", check);
			return "redirect:/launchRetry";
		}

		List<InstanceDataMultiProvider> inputList = JsonSplitter.splitInstanceDataMultiProvider(instanceDataMultiProvider, scenario);
		
		if(inputList.size() > 1){
			List<String> providersList = inputList.stream().map(InstanceDataMultiProvider::getProvider).collect(Collectors.toList());
			if(!minNumTxt(providersList,pathList)){
				deleteUploadedFiles(pathList);
				model.addAttribute("message", "Not enough TXT files selected.\nFor each provider in your JSON there must be 2 TXT files containing in their name the provider name.");
				return "redirect:/launchRetry";
			}
		}
		
		List<SimulationsManager> simManagerList = initializeSimManagers(inputList);
		List<String> txtFoldersList = new ArrayList<>();
		
		for(SimulationsManager sm: simManagerList){
			sm.setInputFileName(Paths.get(instanceDataMultiProviderPath).getFileName().toString());
			InstanceDataMultiProvider input = sm.getInputData();
			String txtFolder = new String();
			try{
				txtFolder = fileUtility.createInputSubFolder();
				txtFoldersList.add(txtFolder);
			}catch(Exception e){
				deleteUploadedFiles(pathList);
				deleteUploadedFiles(txtFoldersList);
				redirectAttrs.addAttribute("message", "Too many folders for TXTs with the same name have been created!");
				return "redirect:/launchRetry";
			}
			for (Entry<String, Map<String, Map<String, JobProfile>>> jobIDs : input.getMapJobProfiles().getMapJobProfile().entrySet()) {
				for (Entry<String, Map<String, JobProfile>> provider : jobIDs.getValue().entrySet()) {
					for (Entry<String, JobProfile> typeVMs : provider.getValue().entrySet()) {
						
						String secondPartOfTXTName = getSecondPartOfReplayersName(jobIDs.getKey(), provider.getKey(), typeVMs.getKey());
						
						List<String> txtToBeSaved = pathList.stream().filter(s->s.contains(secondPartOfTXTName)).filter(s->s.contains(input.getId())).collect(Collectors.toList());
						if(txtToBeSaved.isEmpty()){
							deleteUploadedFiles(pathList);
							deleteUploadedFiles(txtFoldersList);
							model.addAttribute("message", "Missing TXT file for Instance:"+input.getId()+", Job: "+jobIDs.getKey()+", Provider:"+provider.getKey()+", TypeVM:"+typeVMs.getKey());
							return "redirect:/launchRetry";
						}
						
						for(String srcPath:txtToBeSaved){
							File src = new File(srcPath);
							
							String fileContent=new String();
							try {
								fileUtility.copyFile(srcPath, txtFolder+src.getName());
								fileContent = new String(Files.readAllBytes(Paths.get(srcPath)));
							} catch (IOException e) {
								deleteUploadedFiles(pathList);
								deleteUploadedFiles(txtFoldersList);
								model.addAttribute("message", "Problem with TXT paths. [TXT file for Instance:"+input.getId()+", Job: "+jobIDs.getKey()+", Provider:"+provider.getKey()+", TypeVM:"+typeVMs.getKey()+"]");
								return "redirect:/launchRetry";
							}
							if(fileContent.length()==0){
								deleteUploadedFiles(pathList);
								deleteUploadedFiles(txtFoldersList);
								model.addAttribute("message", "Missing TXT file for Instance:"+input.getId()+", Job: "+jobIDs.getKey()+", Provider:"+provider.getKey()+", TypeVM:"+typeVMs.getKey());
								return "redirect:/launchRetry";
							}
							sm.addInputFolder(txtFolder);
							sm.setNumCompletedSimulations(0);
							sm.buildExperiments();
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
		if(pathList.size() == 0){
			deleteUploadedFiles(pathList);
			redirectAttrs.addAttribute("message", "You haven't submitted any file!");
			return "redirect:/launchRetry";
		}
		if (instanceDataPath == null) {
			redirectAttrs.addAttribute("message", "Select a Json file!");
			deleteUploadedFiles(pathList);
			return "redirect:/launchRetry";
		}

		InstanceDataMultiProvider instanceData = validator.objectFromPath(Paths.get(instanceDataPath), InstanceDataMultiProvider.class).get();

		String check = scenarioValidation(instanceData, scenario);
		if(!check.equals("ok")) {
			redirectAttrs.addAttribute("message", check);
			deleteUploadedFiles(pathList);
			return "redirect:/launchRetry";
		}

		List<InstanceDataMultiProvider> inputList = new ArrayList<>();
		inputList.add(instanceData);
		

		if(inputList.size() > 1){
			List<String> providersList = inputList.stream().map(InstanceDataMultiProvider::getProvider).collect(Collectors.toList());
			if(!minNumTxt(providersList,pathList)){
				deleteUploadedFiles(pathList);
				model.addAttribute("message", "Not enough TXT files selected.\nFor each provider in your JSON there must be 2 TXT files containing in their name the provider name.");
				return "redirect:/launchRetry";
			}
		}
		
		List<SimulationsManager> simManagerList = initializeSimManagers(inputList);
		List<String> txtFoldersList = new ArrayList<>();

		for(SimulationsManager sm : simManagerList){
			sm.setInputFileName(Paths.get(instanceDataPath).getFileName().toString());
			
			String txtFolder = new String();
			try{
				txtFolder = fileUtility.createInputSubFolder();
				txtFoldersList.add(txtFolder);
			}catch(Exception e){
				deleteUploadedFiles(pathList);
				deleteUploadedFiles(txtFoldersList);
				redirectAttrs.addAttribute("message", "Too many folders for TXTs with the same name have been created!");
				return "redirect:/launchRetry";
			}
			
			InstanceDataMultiProvider input = sm.getInputData();
			for (Entry<String, Map<String, Map<String, JobProfile>>> jobIDs : input.getMapJobProfiles().getMapJobProfile().entrySet()) {
				for (Entry<String, Map<String, JobProfile>> provider : jobIDs.getValue().entrySet()) {
					for (Entry<String, JobProfile> typeVMs : provider.getValue().entrySet()) {
						
						String secondPartOfTXTName = getSecondPartOfReplayersName(jobIDs.getKey(), provider.getKey(), typeVMs.getKey());
						
						
						List<String> txtToBeSaved = pathList.stream().filter(s->s.contains(secondPartOfTXTName)).filter(s->s.contains(input.getId())).collect(Collectors.toList());
						if(txtToBeSaved.isEmpty()){
							deleteUploadedFiles(pathList);
							deleteUploadedFiles(txtFoldersList);
							model.addAttribute("message", "Missing TXT file for Instance:"+input.getId()+", Job: "+jobIDs.getKey()+", Provider:"+provider.getKey()+", TypeVM:"+typeVMs.getKey());
							return "redirect:/launchRetry";
						}
						
						for(String srcPath:txtToBeSaved){
							File src = new File(srcPath);
							
							String fileContent=new String();
							try {
								fileUtility.copyFile(srcPath, txtFolder+src.getName());
							} catch (IOException e) {
								deleteUploadedFiles(pathList);
								deleteUploadedFiles(txtFoldersList);
								model.addAttribute("message", "Problem with TXT paths. [TXT file for Instance:"+input.getId()+", Job: "+jobIDs.getKey()+", Provider:"+provider.getKey()+", TypeVM:"+typeVMs.getKey()+"]");
								return "redirect:/launchRetry";
							}
							if(fileContent.length()==0){
								deleteUploadedFiles(pathList);
								deleteUploadedFiles(txtFoldersList);
								model.addAttribute("message", "Missing TXT file for Instance:"+input.getId()+", Job: "+jobIDs.getKey()+", Provider:"+provider.getKey()+", TypeVM:"+typeVMs.getKey());
								return "redirect:/launchRetry";
							}
							sm.addInputFolder(txtFolder);
							sm.setNumCompletedSimulations(0);
							sm.buildExperiments();
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


	/**
	 * Precondition TXT file name must respect this regex:
	 * (MAP|REDUCE)[CUSTOM_INTEGER](PROVIDER)(ID)(VM_TYPE)(CLASS_ID)
	 * 
	 * Custom integer used in case of DAGs
	 * Provider mandatory in Public Cloud with InstanceDataMultiProvider
	 * "Map",input.getId(), jobIDs.getKey(), typeVMs.getKey()
	 * EX: 32_h8_D500000.0MapJ2inHouse5xlarge.txt
	 */
	public String getReplayersFileName(String typeOfFile,String idA, String idC, String provider, String vmType){ //TODO move to shared
		return idA+typeOfFile+getSecondPartOfReplayersName(idC,provider,vmType)+".txt";
	}
	
	private String getSecondPartOfReplayersName(String idC, String provider, String vmType){
		return "J"+idC+provider+vmType;
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
		String folder = fileUtility.generateUniqueString();

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

	
	/**
	 * With multi provider different TXTs should be provided
	 */
	private boolean minNumTxt(List<String> providers,ArrayList<String> pathList){
		for(String provider:providers){
			if(pathList.stream().filter(s->s.contains(provider)).count()<2) return false;
		}
		return true;
	}
}
