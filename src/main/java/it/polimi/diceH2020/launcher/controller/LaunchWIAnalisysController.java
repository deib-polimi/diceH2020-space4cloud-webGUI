package it.polimi.diceH2020.launcher.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import it.polimi.diceH2020.launcher.model.SimulationsWIManager;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.service.Validator;
import it.polimi.diceH2020.launcher.utility.policy.DeletionPolicy;

@SessionAttributes("sim_manager") // it will persist in each browser tab,
								  // resolved with
								  // http://stackoverflow.com/questions/368653/how-to-differ-sessions-in-browser-tabs/11783754#11783754
@Controller
@RequestMapping("/launch/wi")
public class LaunchWIAnalisysController {

	@Autowired
	Validator validator;
	
	@Autowired
	private DeletionPolicy policy;

	@Autowired
	private DiceService ds;

	@ModelAttribute("sim_manager")
	public SimulationsWIManager createSim_manager() {
		return new SimulationsWIManager();
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
			@ModelAttribute("sim_manager") SimulationsWIManager simManager, 
			@ModelAttribute("inputPath") String inputSolPath,
			@ModelAttribute("pathFile1") String mapFile,
			@ModelAttribute("pathFile2") String rsFile) {
		
		File json = new File(inputSolPath);
		File map  = new File(mapFile); 
		File red = new File(rsFile); 
		policy.markForDeletion(json);
		policy.markForDeletion(map);
		policy.markForDeletion(red);

		if (inputSolPath != null){
			if (simManager == null) {
				simManager = new SimulationsWIManager();
				model.addAttribute("sim_manager", simManager);
			}
			Solution inputJson = validator.objectFromPath(Paths.get(inputSolPath), Solution.class).get();
			simManager.setInputJson(inputJson);
			simManager.setInputFileName(inputSolPath);
			String mapContent = "";
			String rsContent = "";
			try {
				mapContent = new String(Files.readAllBytes(Paths.get(mapFile)));
				rsContent = new String(Files.readAllBytes(Paths.get(rsFile)));

				simManager.addInputFiles(mapFile.split("/")[1],rsFile.split("/")[1],mapContent,rsContent); 
				System.out.println("Sim manager inputs:"+simManager.getInputFiles().get(0)[0]+","+simManager.getInputFiles().get(0)[1]);
				policy.delete(json);
				policy.delete(map);
				policy.delete(red);
				return "simulationSetup";
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		policy.delete(json);
		policy.delete(map);
		policy.delete(red);
		return "error";
	}

	@RequestMapping(value = "/simulations", method = RequestMethod.POST)
	public String checkSimulationsManagerForm(@Valid @ModelAttribute("sim_manager") SimulationsWIManager simManager, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) return "simulationSetup";

		Boolean allCommon = true;
		if (simManager.getThinkTime() == null) allCommon = false;
		if (simManager.getMinNumVMs() == null) allCommon = false;
		if (simManager.getMaxNumVMs() == null) allCommon = false;
		if (simManager.getMinNumUsers() == null) allCommon = false;
		if (simManager.getMaxNumUsers() == null) allCommon = false;

		if (allCommon) { // the simulation can start
			fixParams(simManager);// just in case
			simManager.buildExperiments();
			simManager.setNumCompletedSimulations(0);
			//simManager.setSize();
			return "createSimulations";
		} else {
			return "error";
		}
	}

	@RequestMapping(value = "createSimulations", method = RequestMethod.POST)
	public String runSimulations(@ModelAttribute("sim_manager") SimulationsWIManager expManager, SessionStatus sessionStatus) {
		ds.simulation(expManager);
		sessionStatus.setComplete();
		return "redirect:/resWI";
	}

	private void fixParams(SimulationsWIManager simManager) {
		if (simManager.getMinNumVMs() != null && simManager.getMaxNumVMs() != null) {
			if (simManager.getMinNumVMs() > simManager.getMaxNumVMs()) {
				simManager.setMinNumVMs(simManager.getMaxNumVMs());
			}
		}
		if (simManager.getMinNumUsers() != null && simManager.getMaxNumUsers() != null) {
			if (simManager.getMinNumUsers() > simManager.getMaxNumUsers()) {
				simManager.setMinNumUsers(simManager.getMaxNumUsers());
			}
		}
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
