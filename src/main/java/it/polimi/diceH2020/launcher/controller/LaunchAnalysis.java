package it.polimi.diceH2020.launcher.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.ClassParametersMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobProfilesMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.PrivateCloudParameters;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.PublicCloudParametersMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.VMConfigurationsMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.service.Validator;
import it.polimi.diceH2020.launcher.utility.JsonMapper;

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

	@ModelAttribute("sim_manager")
	public SimulationsManager createSim_manager() {
		return new SimulationsManager();
	}

//	@ModelAttribute("sim_class")
//	public InteractiveExperiment getSimClass() {
//		return new InteractiveExperiment();
//	}

	@RequestMapping(value = "/error", method = RequestMethod.GET)
	public String showSimulationsManagerForm() {
		return "error";
	}

	// TODO still useful??
	@RequestMapping(value = "/simulationSetupWithMultipleJson", method = RequestMethod.GET)
	public String showSimulationsManagerForm2(SessionStatus sessionStatus, Model model,
			@ModelAttribute("classParametersPath") String classParametersPath,
			@ModelAttribute("jobProfilePath") String jobProfilePath,
			@ModelAttribute("privateCloudParametersPath") String privateCloudParametersPath,
			@ModelAttribute("publicCloudParametersPath") String publicCloudParametersPath,
			@ModelAttribute("vmConfigurationsPath") String vmConfigurationsPath,
			@ModelAttribute("pathFile1") String mapFile, @ModelAttribute("pathFile2") String rsFile,
			@ModelAttribute("scenario") String scenarioString) {

		Scenarios scenario = Scenarios.valueOf(scenarioString);
		model.addAttribute("scenario", scenario);

		if (classParametersPath != null && jobProfilePath != null) {

			if (!validator.validateClassParameters(Paths.get(classParametersPath))) {
				model.addAttribute("message",
						"Class Parameter uploaded Json wasn't valid (you've inserted another type of valid json )!");
				return "launchSimulation_FileUpload";
			}

			if (!validator.validateClassParameters(Paths.get(jobProfilePath))) {
				model.addAttribute("message",
						"Job Profile uploaded Json wasn't valid (you've inserted another type of valid json )!");
				return "launchSimulation_FileUpload";
			}

			ClassParametersMap classParametersMap = validator
					.objectFromPath(Paths.get(classParametersPath), ClassParametersMap.class).get();
			JobProfilesMap jobProfilesMap = validator.objectFromPath(Paths.get(jobProfilePath), JobProfilesMap.class)
					.get();
			PublicCloudParametersMap publicCloudParametersMap = null;
			PrivateCloudParameters privateCloudParametersMap = null;
			VMConfigurationsMap vmConfigurationsMap = null;

			switch (scenario) {
			case PrivateAdmissionControl:
				if (!validator.validateClassParameters(Paths.get(privateCloudParametersPath))) {
					model.addAttribute("message",
							"Private Cloud Parameters uploaded Json wasn't valid (you've inserted another type of valid json )!");
					return "launchSimulation_FileUpload";
				}
				if (!validator.validateClassParameters(Paths.get(vmConfigurationsPath))) {
					model.addAttribute("message",
							"VM Configurations uploaded Json wasn't valid (you've inserted another type of valid json )!");
					return "launchSimulation_FileUpload";
				}
				privateCloudParametersMap = validator
						.objectFromPath(Paths.get(privateCloudParametersPath), PrivateCloudParameters.class).get();
				vmConfigurationsMap = validator
						.objectFromPath(Paths.get(vmConfigurationsPath), VMConfigurationsMap.class).get();
				break;
			case PrivateNoAdmissionControl:
				if (!validator.validateClassParameters(Paths.get(vmConfigurationsPath))) {
					model.addAttribute("message",
							"VM Configurations uploaded Json wasn't valid (you've inserted another type of valid json )!");
					return "launchSimulation_FileUpload";
				}
				vmConfigurationsMap = validator
						.objectFromPath(Paths.get(vmConfigurationsPath), VMConfigurationsMap.class).get();
				break;
			case PublicPeakWorkload:
				if (!validator.validateClassParameters(Paths.get(publicCloudParametersPath))) {
					model.addAttribute("message",
							"Public Cloud Parameters uploaded Json wasn't valid (you've inserted another type of valid json )!");
					return "launchSimulation_FileUpload";
				}
				publicCloudParametersMap = validator
						.objectFromPath(Paths.get(publicCloudParametersPath), PublicCloudParametersMap.class).get();
				break;
			case PublicAvgWorkLoad:
				break;
			default:
				new Exception("Error with scenario files");
				break;
			}

			InstanceDataMultiProvider inputData = new InstanceDataMultiProvider("id", jobProfilesMap,
					classParametersMap, publicCloudParametersMap, privateCloudParametersMap, vmConfigurationsMap);
			if (!inputData.validate()) {
				model.addAttribute("message", "Json aren't consistent!");
				return "launchSimulation_FileUpload";
			}

			return "runSimulations";
		}
		return "error";
	}

	@RequestMapping(value = "/simulationSetup", method = RequestMethod.GET)
	public String showSimulationsManagerForm(SessionStatus sessionStatus, Model model,
			@ModelAttribute("instanceDataMultiProvider") String instanceDataMultiProviderPath,
			@ModelAttribute("pathFile1") String mapFile, @ModelAttribute("pathFile2") String rsFile,
			@ModelAttribute("scenario") String scenarioString) {

		Scenarios scenario = Scenarios.valueOf(scenarioString);
		model.addAttribute("scenario", scenario);

		if (instanceDataMultiProviderPath == null) {
			model.addAttribute("message", "Select a Json file!");
			return "launchSimulation_FileUpload";
		}

		if (!validator.validateInstanceDataMultiProvider(Paths.get(instanceDataMultiProviderPath))) {
			model.addAttribute("message", "The uploaded Json isn't valid!");
			return "launchSimulation_FileUpload";
		}

		InstanceDataMultiProvider instanceDataMultiProvider = validator.objectFromPath(Paths.get(instanceDataMultiProviderPath), InstanceDataMultiProvider.class).get();
		
		String check = scenarioValidation(instanceDataMultiProvider, scenario);
		if(!check.equals("ok")) {
			model.addAttribute("message", check);
			return "launchSimulation_FileUpload";
		}
		
		List<InstanceData> inputList = JsonMapper.getInstanceDataList(instanceDataMultiProvider, scenario);
		List<SimulationsManager> simManagerList = initializeSimManagers(inputList);
		
		for(SimulationsManager sm : simManagerList){
			sm.setInputFileName(Paths.get(instanceDataMultiProviderPath).getFileName().toString());
			try {
				String mapContent = new String(Files.readAllBytes(Paths.get(mapFile)));
				String rsContent = new String(Files.readAllBytes(Paths.get(rsFile)));
				sm.addInputFiles(mapFile.split("/")[1],rsFile.split("/")[1],mapContent,rsContent);
				System.out.println("Sim manager inputs:"+sm.getInputFiles().get(0)[0]+","+sm.getInputFiles().get(0)[1]);
				
				sm.setNumCompletedSimulations(0);
				sm.buildExperiments();
				ds.simulation(sm);
			} catch (IOException e) {
				model.addAttribute("message","Error with txt files!");
				return "launchSimulation_FileUpload";
			}
		}
		model.addAttribute("simManagersList",simManagerList);
		//return "launchSimulation_Recap";
		return "redirect:/";
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
				if(instanceDataMultiProvider.getMapPublicCloudParameters()==null){
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
	
	private List<SimulationsManager> initializeSimManagers(List<InstanceData> inputList){
		List<SimulationsManager> simManagerList = new ArrayList<SimulationsManager>();
		String folder = generateUniqueString();
		
		for(InstanceData instanceData : inputList){
			SimulationsManager simManager = new SimulationsManager();
			simManager.setInputData(instanceData);
			simManager.setProvider(instanceData.getProvider());
			simManager.setScenario(instanceData.getScenario());
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
	
	private String generateUniqueString() {
		//String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		Date dNow = new Date( );
	    SimpleDateFormat ft = new SimpleDateFormat ("Edd-MM-yyyy_HH-mm-ss");
	    Random random = new Random();
	    String id = ft.format(dNow)+random.nextInt(99999);
	    return id;
	}
}
