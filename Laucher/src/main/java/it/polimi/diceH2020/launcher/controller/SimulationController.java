package it.polimi.diceH2020.launcher.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.service.Validator;

@SessionAttributes("sim_manager") // it will persist in each browser tab,
									// resolved with
									// http://stackoverflow.com/questions/368653/how-to-differ-sessions-in-browser-tabs/11783754#11783754
@Controller
@RequestMapping("/sim")
public class SimulationController {

	@Autowired
	Validator validator;

	@Autowired
	private DiceService ds;

	@ModelAttribute("sim_manager")
	public SimulationsManager createSim_manager() {
		return new SimulationsManager();
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
			@ModelAttribute("sim_manager") SimulationsManager simManager, 
			@ModelAttribute("inputPath") String inputSolPath,
			@ModelAttribute("pathFile1") String mapFile,
			@ModelAttribute("pathFile2") String rsFile) {

		if (inputSolPath == null) return "error";
		if (simManager == null) {
			simManager = new SimulationsManager();
			 model.addAttribute("sim_manager", simManager);
		}
		//
		Solution inputSol = validator.objectFromPath(Paths.get(inputSolPath), Solution.class).get();

		simManager.setInputSolution(inputSol);
		
		String mapContent = "";
		String rsContent = "";
		try {
			mapContent = new String(Files.readAllBytes(Paths.get(mapFile)));
			rsContent = new String(Files.readAllBytes(Paths.get(rsFile)));
		} catch (IOException e) {
			return "error";
		}
		
		simManager.setMapFile(mapContent);
		simManager.setRsFile(rsContent);
		return "simulationSetup";
	}

	@RequestMapping(value = "/simulations", method = RequestMethod.POST)
	public String checkSimulationsManagerForm(@Valid @ModelAttribute("sim_manager") SimulationsManager simManager, BindingResult bindingResult) {
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
			return "createSimulations";
		} else {
			return "error";
		}
	}



	@RequestMapping(value = "createSimulations", method = RequestMethod.POST)
	public String runSimulations(@ModelAttribute("sim_manager") SimulationsManager expManager, SessionStatus sessionStatus) {
		ds.simulation(expManager);
		sessionStatus.setComplete();
		return "redirect:/totalExperimentList";
	}

	private void fixParams(SimulationsManager simManager) {
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
