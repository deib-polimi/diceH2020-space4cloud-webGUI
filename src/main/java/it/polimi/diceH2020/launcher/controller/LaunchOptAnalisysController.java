package it.polimi.diceH2020.launcher.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.WebRequest;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsOptManager;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.service.Validator;
import it.polimi.diceH2020.launcher.utility.policy.DeletionPolicy;

@SessionAttributes("sim_manager") // it will persist in each browser tab,
								  // resolved with
								  // http://stackoverflow.com/questions/368653/how-to-differ-sessions-in-browser-tabs/11783754#11783754
@Controller
@RequestMapping("/launch/opt")
public class LaunchOptAnalisysController {
	
	@Autowired
	Validator validator;
	
	@Autowired
	private DeletionPolicy policy;
	
	@Autowired
	private DiceService ds;
	
	@ModelAttribute("sim_manager")
	public SimulationsOptManager createSim_manager() {
		return new SimulationsOptManager();
	}

	@ModelAttribute("sim_class")
	public InteractiveExperiment getSimClass() {
		return new InteractiveExperiment();
	}

	@RequestMapping(value = "/error", method = RequestMethod.GET)
	public String showSimulationsManagerForm() {
		return "error";
	}

	@RequestMapping(value = "/simulationSetup", method = RequestMethod.GET)
	public String showSimulationsManagerForm(SessionStatus sessionStatus, Model model,  WebRequest request,
			@ModelAttribute("sim_manager") SimulationsOptManager simManager, 
			@ModelAttribute("pathList") ArrayList<ArrayList<String>> pathList, @ModelAttribute("cloudType") String cloudType) {
		
		String folder = generateUniqueString();
		
		
		if(pathList.size() == 0)return "error";
		for(int i=0; i<pathList.size();i++){
			
			if (pathList.get(i).get(0) == null || !pathList.get(i).get(0).contains(".json")) return "error";
			
			ArrayList<String> tmpList = pathList.get(i); 
			String inputSolPath = tmpList.get(0);
			File tmpFile = new File(inputSolPath);
			policy.markForDeletion(tmpFile);
			if (simManager == null) {
				simManager = new SimulationsOptManager();
				model.addAttribute("sim_manager", simManager);
			}
			InstanceData inputData = validator.objectFromPath(Paths.get(inputSolPath), InstanceData.class).get();
			simManager.setInputData(inputData);
			simManager.setInputFileName(Paths.get(inputSolPath).getFileName().toString());
			simManager.setProvider(inputData.getProvider());
			simManager.setGamma(inputData.getGamma());
			simManager.setFolder(folder);
			
			try {
				simManager.setCloudType(CloudType.valueOf(cloudType));
			}catch(Exception exc){
			}
			
			policy.delete(tmpFile);
			tmpList.remove(0);
			int j = 0;
			while(tmpList.size()!=0){
				String mapFile,rsFile,mapFileName,rsFileName,mapFileContent,rsFileContent;
				mapFile=rsFile=mapFileName=rsFileName=mapFileContent=rsFileContent = new String();
				tmpFile = new File(tmpList.get(j));
				policy.markForDeletion(tmpFile);
				if(tmpList.get(j).contains("Map")){
					mapFile = tmpList.get(j);
					mapFileName = Paths.get(mapFile).getFileName().toString();
					try {
						mapFileContent = new String(Files.readAllBytes(Paths.get(mapFile)));
					} catch (IOException e) {
						return "error";
					}
					String mirrorName = mapFile.replace("Map", "RS");
					int indexRS = tmpList.indexOf(mirrorName);
					if(indexRS!=-1){
						File tmpMirrorFile = new File(mirrorName);
						policy.markForDeletion(tmpMirrorFile);
						rsFile = tmpList.get(indexRS);
						rsFileName = Paths.get(rsFile).getFileName().toString();
						try {
							rsFileContent = new String(Files.readAllBytes(Paths.get(rsFile)));
						} catch (IOException e) {
							return "error";
						}
						policy.delete(tmpMirrorFile);
						tmpList.remove(indexRS);
						
					}//else{rsFileContent = "" rsFileName=""
					policy.delete(tmpFile);
					tmpList.remove(j);
				}else 
					if(tmpList.get(j).contains("RS")){
						//and mapFileContent = "" name=""
						rsFile = tmpList.get(j);
						try {
							rsFileContent = new String(Files.readAllBytes(Paths.get(rsFile)));
						} catch (IOException e) {
							return "error";
						}
						policy.delete(tmpFile);
						tmpList.remove(j);
					}else{
						return "error";
					}
				
				simManager.addInputFiles(mapFileName,rsFileName,mapFileContent,rsFileContent);
			}
			simManager.buildExperiments();
			simManager.setNumCompletedSimulations(0);
			//simManager.setSize();
			
			ds.simulation(simManager);
			sessionStatus.setComplete();
			request.removeAttribute("sim_manager", WebRequest.SCOPE_SESSION);
			simManager = new SimulationsOptManager();
		}
		return "redirect:/resOpt";
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
