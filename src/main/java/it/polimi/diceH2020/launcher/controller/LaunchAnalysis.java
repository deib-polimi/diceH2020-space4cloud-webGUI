/*
Copyright 2017 Eugenio Gianniti
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
import it.polimi.diceH2020.launcher.utility.Compressor;
import it.polimi.diceH2020.launcher.utility.FileNameClashException;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

@SessionAttributes("sim_manager") // it will persist in each browser tab,
// resolved with
// http://stackoverflow.com/questions/368653/how-to-differ-sessions-in-browser-tabs/11783754#11783754
@Controller
@RequestMapping("/launch")
public class LaunchAnalysis {

	private final Logger logger = Logger.getLogger (getClass ());

	@Autowired
	private Validator validator;

	@Autowired
	private DiceService ds;

	@Autowired
	private FileUtility fileUtility;

	private List<String> inputPaths;
	private List<File> generatedFiles;
	private File inputJSONFile;

	@ModelAttribute("sim_manager")
	public SimulationsManager createSim_manager() {
		return new SimulationsManager();
	}

	@RequestMapping(value = "/error", method = RequestMethod.GET)
	public String showSimulationsManagerForm() {
		return "error";
	}

	@RequestMapping(value = "/simulationSetup", method = RequestMethod.GET)
	public synchronized String showSimulationsManagerForm(SessionStatus sessionStatus, Model model,
														  @ModelAttribute("instanceDataMultiProvider")
																  String instanceDataMultiProviderPath,
														  @ModelAttribute("pathList") ArrayList<String> pathList,
														  @ModelAttribute("scenario") String scenarioString,
														  RedirectAttributes redirectAttrs) {

		inputPaths = pathList;
		inputJSONFile = new File (instanceDataMultiProviderPath);

		Scenarios scenario = Scenarios.valueOf(scenarioString);
		model.addAttribute("scenario", scenario);
		redirectAttrs.addAttribute("scenario", scenario);

		if (pathList.size() == 0) {
			cleanup(false);
			String message = "You haven't submitted any file!";
			logger.error (message);
			redirectAttrs.addAttribute("message", message);
			return "redirect:/launchRetry";
		}
		if (instanceDataMultiProviderPath == null) {
			cleanup(false);
			String message = "Select a Json file!";
			logger.error (message);
			redirectAttrs.addAttribute("message", message);
			return "redirect:/launchRetry";
		}

		Optional<InstanceDataMultiProvider> idmp =
				validator.readInstanceDataMultiProvider(Paths.get(instanceDataMultiProviderPath));

		if (idmp.isPresent()) {
			if (!idmp.get().validate()) {
				cleanup(false);
				logger.error (idmp.get().getValidationError());
				redirectAttrs.addAttribute("message", idmp.get().getValidationError());
				return "redirect:/launchRetry";
			}
		} else {
			cleanup(false);
			String message = "Error with InstanceDataMultiProvider";
			logger.error (message);
			redirectAttrs.addAttribute("message", message);
			return "redirect:/launchRetry";
		}

		InstanceDataMultiProvider instanceDataMultiProvider = idmp.get();

		String check = scenarioValidation(instanceDataMultiProvider, scenario);
		if (! check.equals("ok")) {
			cleanup(false);
			logger.error (check);
			redirectAttrs.addAttribute("message", check);
			return "redirect:/launchRetry";
		}

		List<InstanceDataMultiProvider> inputList =
				JsonSplitter.splitInstanceDataMultiProvider(instanceDataMultiProvider, scenario);

		if (inputList.size() > 1) {
			List<String> providersList = inputList.stream().map(InstanceDataMultiProvider::getProvider).collect(Collectors.toList());
			if (! minNumTxt(providersList, pathList)) {
				cleanup(false);
				String message = "Not enough TXT files selected.\nFor each provider in your JSON there must be 2 TXT files containing in their name the provider name.";
				logger.error (message);
				redirectAttrs.addAttribute("message", message);
				return "redirect:/launchRetry";
			}
		}

		List<SimulationsManager> simManagerList = initializeSimManagers(inputList);
		generatedFiles = new ArrayList<>();

		for (SimulationsManager sm: simManagerList) {
			sm.setInputFileName(Paths.get(instanceDataMultiProviderPath).getFileName().toString());
			InstanceDataMultiProvider input = sm.getInputData();

			File txtFolder;
			try {
				txtFolder = fileUtility.createInputSubFolder();
				generatedFiles.add(txtFolder);
			} catch (FileNameClashException e) {
				cleanup(true);
				String message = "Too many folders for TXTs with the same name have been created!";
				logger.error (message, e);
				redirectAttrs.addAttribute("message", message);
				return "redirect:/launchRetry";
			}

			for (Entry<String, Map<String, Map<String, JobProfile>>> jobIDs : input.getMapJobProfiles().getMapJobProfile().entrySet()) {
				for (Entry<String, Map<String, JobProfile>> provider : jobIDs.getValue().entrySet()) {
					for (Entry<String, JobProfile> typeVMs : provider.getValue().entrySet()) {

						String secondPartOfTXTName =
								getSecondPartOfReplayersName(jobIDs.getKey(), provider.getKey(), typeVMs.getKey());

						List<String> txtToBeSaved = pathList.stream().filter(s -> s.contains(secondPartOfTXTName))
								.filter(s->s.contains(input.getId())).collect(Collectors.toList());

						if (txtToBeSaved.isEmpty()) {
							cleanup(true);
							String message = "Missing TXT file for Instance: "+input.getId()+", Job: "+jobIDs.getKey()+", Provider:"+provider.getKey()+", TypeVM:"+typeVMs.getKey();
							logger.error (message);
							redirectAttrs.addAttribute("message", message);
							return "redirect:/launchRetry";
						}

						for (String srcPath: txtToBeSaved) {
							File src = new File(srcPath);

							String fileContent;
							try {
								fileContent = String.join("\n", Files.readAllLines(Paths.get(srcPath)));
								String compressedContent = Compressor.compress(fileContent);

								File generatedFile = fileUtility.createInputFile(txtFolder, src.getName());
								fileUtility.writeContentToFile(compressedContent, generatedFile);

								generatedFiles.add(generatedFile);
							} catch (FileNameClashException e) {
								cleanup(true);
								redirectAttrs.addAttribute("message", e.getMessage());
								logger.error("Cannot create file due to filename clash", e);
								return "redirect:/launchRetry";
							} catch (IOException e) {
								cleanup(true);
								String message = "Problem with TXT paths. [TXT file for Instance:"+input.getId()+", Job: "+jobIDs.getKey()+", Provider:"+provider.getKey()+", TypeVM:"+typeVMs.getKey()+"]";
								logger.error (message, e);
								redirectAttrs.addAttribute("message", message);
								return "redirect:/launchRetry";
							}

							if (fileContent.isEmpty ()) {
								cleanup(true);
								String message = "Missing TXT file content for Instance: "+input.getId()+", Job: "+jobIDs.getKey()+", Provider:"+provider.getKey()+", TypeVM:"+typeVMs.getKey();
								logger.error (message);
								redirectAttrs.addAttribute("message", message);
								return "redirect:/launchRetry";
							}

							sm.addInputFolder(txtFolder.getPath ());
							sm.setNumCompletedSimulations(0);
							sm.buildExperiments();
						}
					}
				}
			}
		}

		cleanup(false);

		for (SimulationsManager sm : simManagerList) {
			ds.simulation(sm);
		}
		model.addAttribute("simManagersList", simManagerList);
		return "redirect:/";
	}

	private String getSecondPartOfReplayersName(String idC, String provider, String vmType){
		return "J"+idC+provider+vmType;
	}

	private String scenarioValidation(InstanceDataMultiProvider instanceDataMultiProvider, Scenarios scenario) {
		String returnString = "ok";
		if(instanceDataMultiProvider.getMapJobProfiles()==null || instanceDataMultiProvider.getMapClassParameters()==null){
			returnString = "Json is missing some required parameters(MapJobProfiles or MapClassParameters)!";
		} else switch (scenario) {
			case PrivateAdmissionControl:
				if (instanceDataMultiProvider.getPrivateCloudParameters()==null||instanceDataMultiProvider.getMapVMConfigurations()==null) {
					returnString = "Json is missing some required parameters(PrivateCloudParameters or MapVMConfigurations)!";
				} else if (!instanceDataMultiProvider.getPrivateCloudParameters().validate()) {
					returnString = "Private Cloud Parameters uploaded in Json aren't valid!";
				} else if (!instanceDataMultiProvider.getMapVMConfigurations().validate()) {
					returnString = "VM Configurations uploaded in Json aren't valid!";
				} else if (instanceDataMultiProvider.getProvidersList().size() != 1) {
					returnString = "A private scenario cannot have multiple providers!(call you providers:\"inHouse\")";
				}
				break;

			case PrivateNoAdmissionControl:
				if (instanceDataMultiProvider.getMapVMConfigurations()==null) {
					returnString = "Json is missing some required parameters(MapVMConfigurations)!";
				} else if (instanceDataMultiProvider.getMapVMConfigurations().getMapVMConfigurations()==null) {
					returnString = "Json is missing some required parameters(MapVMConfigurations)!";
				} else if (!instanceDataMultiProvider.getMapVMConfigurations().validate()) {
					returnString = "VM Configurations uploaded in Json aren't valid!";
				} else if (instanceDataMultiProvider.getProvidersList().size() != 1) {
					returnString = "A private scenario cannot have multiple providers!(call you providers:\"inHouse\")";
				}
				break;

			case PublicPeakWorkload:
				if (instanceDataMultiProvider.getMapPublicCloudParameters()==null || instanceDataMultiProvider.getMapPublicCloudParameters().getMapPublicCloudParameters()==null) {
					returnString = "Json is missing some required parameters(MapPublicCloudParameters)!";
				} else if (!instanceDataMultiProvider.getMapPublicCloudParameters().validate()) {
					returnString = "Public Cloud Parameters uploaded in Json aren't valid!";
				}
				break;

			case PublicAvgWorkLoad:
			case StormPublicAvgWorkLoad:
				break;

			default:
				String errorMessage = String.format ("Scenario not implemented: '%s'", scenario.toString ());
				logger.error (errorMessage);
				throw new RuntimeException (errorMessage);
		}
		return returnString;
	}

	private List<SimulationsManager> initializeSimManagers(List<InstanceDataMultiProvider> inputList){
		List<SimulationsManager> simManagerList = new ArrayList<>();
		String folder = fileUtility.generateUniqueString();

		for(InstanceDataMultiProvider instanceData : inputList){
			SimulationsManager simManager = new SimulationsManager();
			simManager.setInputData(instanceData);
			instanceData.getScenario ().ifPresent (simManager::setScenario);
			simManager.setFolder(folder);

			simManagerList.add(simManager);
		}
		return simManagerList;
	}

	private void cleanup(boolean generated) {
		fileUtility.delete(inputJSONFile);
		deleteUploadedFiles(inputPaths);
		if (generated) fileUtility.delete(generatedFiles);
	}

	private void deleteUploadedFiles(List<String> pathList){
		List<File> filesToErase = new ArrayList<>();
		for(String path: pathList){
			filesToErase.add(new File(path));
		}
		fileUtility.delete(filesToErase);
	}

	/**
	 * With multi provider different TXTs should be provided
	 */
	private boolean minNumTxt(List<String> providers, List<String> pathList){
		for (String provider: providers) {
			if (pathList.stream ().filter (s -> s.contains(provider)).count () < 2) return false;
		}
		return true;
	}
}
