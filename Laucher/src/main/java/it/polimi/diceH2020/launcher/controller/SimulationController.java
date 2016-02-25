package it.polimi.diceH2020.launcher.controller;

import java.math.BigDecimal;
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
import it.polimi.diceH2020.launcher.model.Simulations_class;
import it.polimi.diceH2020.launcher.service.DiceService;

@SessionAttributes("sim_manager") // it will persist in each browser tab,
									// resolved with
									// http://stackoverflow.com/questions/368653/how-to-differ-sessions-in-browser-tabs/11783754#11783754
@Controller
@RequestMapping("/sim")
public class SimulationController {

	@Autowired
	private DiceService ds;

	@ModelAttribute("sim_manager")
	public SimulationsManager createSim_manager() {
		return new SimulationsManager();
	}

	@ModelAttribute("sim_class")
	public Simulations_class getSimClass() {
		return new Simulations_class();
	}

	@RequestMapping(value = "/error", method = RequestMethod.GET)
	public String showSimulationsManagerForm() {
		return "error";
	}

	@RequestMapping(value = "/simulationSetup", method = RequestMethod.GET)
	public String showSimulationsManagerForm(SessionStatus sessionStatus, Model model) {
		sessionStatus.setComplete();
		SimulationsManager simManager = new SimulationsManager();
		model.addAttribute("sim_manager", simManager);
		return "simulationSetup";
	}

	@RequestMapping(value = "/simulations", method = RequestMethod.POST)
	public String checkSimulationsManagerForm(@Valid @ModelAttribute("sim_manager") SimulationsManager simManager, 
												BindingResult bindingResult) {
		if (bindingResult.hasErrors()) return "simulationSetup";

		Boolean allCommon = true;
		if (simManager.getThinkTime() == null) allCommon = false;
		if (simManager.getMinNumVMs() == null) allCommon = false;
		if (simManager.getMaxNumVMs() == null) allCommon = false;
		if (simManager.getMinNumUsers() == null) allCommon = false;
		if (simManager.getMaxNumUsers() == null) allCommon = false;

		if (allCommon) { // the simulation can start
			Simulations_class sim_class = new Simulations_class();
			setUpClass(simManager, sim_class);
			setRates(sim_class);
			simManager.getClassList().add(sim_class);

			setUpSize(simManager);
			return "createSimulations";
		} else {
			int b = 0;
			simManager.setNumCompletedSimulations(b);
			return "redirect:/sim/simulations2"; // at list one class
													// -->simulations2 :D
		}
	}

	@RequestMapping(value = "/simulations2", method = RequestMethod.GET)
	public String showSimulationsClassForm(@ModelAttribute("sim_class") Simulations_class sim_class, Model model) {
		if (!model.containsAttribute("sim_manager")) { throw new IllegalArgumentException("Model must contain sim_manager."); }
		SimulationsManager sim_manager = (SimulationsManager) model.asMap().get("sim_manager");

		setUpClass(sim_manager, sim_class);
		return "set_simulations-class_form";
	}

	@RequestMapping(value = "/simulations2", method = RequestMethod.POST)
	public String checkSimulationsClassForm(@Valid @ModelAttribute("sim_class") Simulations_class sim_class, BindingResult bindingResult, Model model) {
		if (!model.containsAttribute("sim_manager")) { // keep it here! with
														// more model attribute
														// as method params
														// spring will bind all
														// common parameters, in
														// order to avoid this
														// keep sim_manager
														// here.
			throw new IllegalArgumentException("Model must contain sim_manager.");
		}
		SimulationsManager sim_manager = (SimulationsManager) model.asMap().get("sim_manager");

		boolean isNull = false;
		try {
			sim_class.getTabID().equalsIgnoreCase(null);
			sim_manager.getTabID().equalsIgnoreCase(null);
		} catch (NullPointerException npe) {
			isNull = true;
		}
		if (isNull) {
			System.out.println("User tried to create simulations in multiple tabs. ");
			return "redirect:/sim/error";
		}

		if (!sim_manager.getTabID().equals(sim_class.getTabID())) {
			System.out.println("User tried to create simulations in multiple tabs. ");
			return "redirect:/sim/error";
		}

		if (bindingResult.hasErrors()) {
			Collection<FieldError> errors = bindingResult.getFieldErrors();
			for (FieldError error : errors) {
				System.out.println(error.toString());
			}
			return "set_simulations-class_form";
		}
		fixLoopParams(sim_class);
		setRates(sim_class);

		int a = sim_manager.getNumCompletedSimulations();
		a++;
		if (a == 1) {
			// sim_manager.getClass_number()) {
			setUpSize(sim_manager);
			sim_manager.setNumCompletedSimulations(a);
			sim_manager.getClassList().add(sim_class);
			return "createSimulations";
		} else {
			sim_manager.setNumCompletedSimulations(a);
			sim_manager.getClassList().add(sim_class);
			return "redirect:/sim/simulations2";
		}
	}

	@RequestMapping(value = "createSimulations", method = RequestMethod.POST)
	public String runSimulations(@ModelAttribute("sim_manager") SimulationsManager sim_manager, SessionStatus sessionStatus) {
		// start simulations
		// printSimulationsClass(sim_manager);
		sim_manager.setModel("V7");

		ds.simulation(sim_manager);
		sessionStatus.setComplete();
		return "redirect:/listV7";
	}

	private void setUpSize(SimulationsManager sim_manager) {
		int size = 1;
		if (sim_manager.getSize() == 1) {
			for (int i = 0; i < sim_manager.getClassList().size(); i++) {
				int maxNumCores = sim_manager.getClassList().get(i).getMaxNumCores();
				int minCores = sim_manager.getClassList().get(i).getMinNumVMs();
				int stepVMs = sim_manager.getStepVMs();
				int maxNumUsers = sim_manager.getClassList().get(i).getMaxNumUsers();
				int minNumUsers = sim_manager.getClassList().get(i).getMinNumUsers();
				int stepUsrs = sim_manager.getStepUsers();

				size *= ((int) Math.floor((maxNumCores - minCores) / stepVMs) + 1) * ((int) Math.floor((maxNumUsers - minNumUsers) / stepUsrs) + 1);
			}
			sim_manager.setSize(size);
		}
	}

	private void setUpClass(SimulationsManager simManager, Simulations_class simClass) {
		fixParams(simManager);

		if (simManager.getThinkTime() != null) simClass.setThinkTime(simManager.getThinkTime());

		if (simManager.getMinNumVMs() != null) simClass.setMinNumVMs(simManager.getMinNumVMs());

		if (simManager.getMaxNumVMs() != null) simClass.setMaxNumCores(simManager.getMaxNumVMs());

		if (simManager.getMinNumUsers() != null) simClass.setMinNumUsers(simManager.getMinNumUsers());

		if (simManager.getMaxNumUsers() != null) simClass.setMaxNumUsers(simManager.getMaxNumUsers());

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

	private void fixLoopParams(Simulations_class sim_class) {
		if (sim_class.getMinNumVMs() != null && sim_class.getMaxNumCores() != null) {
			if (sim_class.getMinNumVMs() > sim_class.getMaxNumCores()) {
				sim_class.setMinNumVMs(sim_class.getMaxNumCores());
			}
		}
		if (sim_class.getMinNumUsers() != null && sim_class.getMaxNumUsers() != null) {
			if (sim_class.getMinNumUsers() > sim_class.getMaxNumUsers()) {
				sim_class.setMinNumUsers(sim_class.getMaxNumUsers());
			}
		}
	}

	private void setRates(Simulations_class sim_class) {
		sim_class.setThinkRate(round((double) 1 / sim_class.getThinkTime(), 7, BigDecimal.ROUND_HALF_UP));
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
