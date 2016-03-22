package it.polimi.diceH2020.launcher.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsOptManager;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.service.Validator;

@SessionAttributes("sim_manager") // it will persist in each browser tab,
								  // resolved with
								  // http://stackoverflow.com/questions/368653/how-to-differ-sessions-in-browser-tabs/11783754#11783754
@Controller
@RequestMapping("/launch/opt")
public class LaunchOptAnalisysController {
	
	@Autowired
	Validator validator;
	
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
	public String showSimulationsManagerForm(SessionStatus sessionStatus, Model model, 
			@ModelAttribute("sim_manager") SimulationsOptManager simManager, 
			@ModelAttribute("inputPath") String inputSolPath,
			@ModelAttribute("pathFile1") String mapFile,
			@ModelAttribute("pathFile2") String rsFile) {

		if (inputSolPath == null) return "error";
		if (simManager == null) {
			simManager = new SimulationsOptManager();
			model.addAttribute("sim_manager", simManager);
		}
		InstanceData inputData = validator.objectFromPath(Paths.get(inputSolPath), InstanceData.class).get();
		simManager.setInputData(inputData);
		
		simManager.setInstanceName(inputData.getId());
		simManager.setProvider(inputData.getProvider());
		simManager.setGamma(inputData.getGamma());
		
		
		String mapContent = "";
		String rsContent = "";
		try {
			mapContent = new String(Files.readAllBytes(Paths.get(mapFile)));
			rsContent = new String(Files.readAllBytes(Paths.get(rsFile)));
		} catch (IOException e) {
			return "error";
		}
		
		simManager.setMapFile(mapContent);
		if(!simManager.getMapFile().isEmpty()){
			simManager.setMapFileEmpty(false);
		}
		simManager.setRsFile(rsContent);
		if(!simManager.getRsFile().isEmpty()){
			simManager.setRsFileEmpty(false);
		}
		simManager.setMapFileName(Paths.get(mapFile).getFileName().toString());
		simManager.setRsFileName(Paths.get(rsFile).getFileName().toString());
		simManager.buildExperiments();
		simManager.setNumCompletedSimulations(0);
		simManager.setSize();
		
		ds.simulation(simManager);
		sessionStatus.setComplete();
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
}
