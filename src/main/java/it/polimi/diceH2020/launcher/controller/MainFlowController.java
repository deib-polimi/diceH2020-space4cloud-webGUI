package it.polimi.diceH2020.launcher.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;


import it.polimi.diceH2020.launcher.FileService;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.InteractiveExperimentRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import it.polimi.diceH2020.launcher.service.DiceService;

//@SessionAttributes("sim_manager") //it will persist in each browser tab, resolved with http://stackoverflow.com/questions/368653/how-to-differ-sessions-in-browser-tabs/11783754#11783754
@Controller
public class MainFlowController {
	@Autowired
	private FileService fileService;
	
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
    	return "home";
    }	
	
	@RequestMapping(value="/launchWI", method=RequestMethod.GET)
    public String launchWI(SessionStatus sessionStatus, Model model){
		if(model.containsAttribute("sim_manager")){
			sessionStatus.isComplete();
		}
    	return "fileUploadWI";
    }	
	@RequestMapping(value="/launchOpt", method=RequestMethod.GET)
    public String launchOpt(SessionStatus sessionStatus, Model model){
		if(model.containsAttribute("sim_manager")){
			sessionStatus.isComplete();
		}
    	return "fileUploadOpt";
    }	
	
	@RequestMapping(value="/list/instances", method=RequestMethod.GET)
	public String listSolutions(Model model) {
			model.addAttribute("sim_manager", fileService.getListFileWithIndex());
			return "solutionList";
	}

	@RequestMapping(value="/resultsWI", method=RequestMethod.GET)
	public String resultsWI(@RequestParam("id") Long id,Model model) {
			SimulationsManager simManager = simulationsManagerRepository.findOne(id);
			model.addAttribute("sim", intExperimentRepository.findBySimulationsManager(simManager));
			return "simWIList";
	}
	
	@RequestMapping(value="/resultsOpt", method=RequestMethod.GET)
	public String resultsOpt(@RequestParam("id") Long id,Model model) {
			SimulationsManager simManager = simulationsManagerRepository.findOne(id);
			model.addAttribute("sim", intExperimentRepository.findBySimulationsManager(simManager));
			return "simOptList";
	}
	
	@RequestMapping(value="/foldersOpt", method=RequestMethod.GET)
	public String foldersOpt(Model model) {
			model.addAttribute("folders", simulationsManagerRepository.findByIdIn(simulationsManagerRepository.findSimManagerGroupedByFolders()));
			return "foldersOfSimOptMan";
	}
	
	@RequestMapping(value="/resWI", method=RequestMethod.GET)
	public String listWi(Model model) {
			model.addAttribute("sim_manager", simulationsManagerRepository.findByType("WI"));
			return "simManagersWIList";
	}
	
	@RequestMapping(value="/resOpt", method=RequestMethod.GET)
	public String listOpt(Model model) {
			model.addAttribute("sim_manager", simulationsManagerRepository.findByType("Opt"));
			return "simManagersOptList";
	}
	
	@RequestMapping(value="/simOptByFolder", method=RequestMethod.GET)
	public String simOptFolderContent(@RequestParam(value="folder") String folder, Model model) {
	    model.addAttribute("sim_manager", simulationsManagerRepository.findByFolder(folder));
	    return "simManagersOptList";
	}
	
	@RequestMapping(value = "/relaunch", method = RequestMethod.GET)
	public String relaunchExperiment (@RequestParam(value="id") Long id,SessionStatus sessionStatus, Model model,HttpServletRequest request) {
		InteractiveExperiment exp = intExperimentRepository.findById(id);
		exp.setExperimentalDuration(0);
		exp.setResponseTime(0d);
		exp.setFinalSolution(new String()); //inverse of e.setSol(sol); 
		if(exp.getState().equals("completed")){
			exp.setDone(false);
			exp.setState("ready");
			exp.setNumSolutions(exp.getNumSolutions()-1); 
			exp.getSimulationsManager().setNumCompletedSimulations(exp.getSimulationsManager().getNumCompletedSimulations()-1);
			if(exp.getSimulationsManager().getState().equals("completed")){
				exp.getSimulationsManager().setState("ready"); //TODO ready?running?partiallyCompleted?
			}
		}
		ds.singleSimulation(exp);
		return "redirect:" + request.getHeader("Referer");
	}
	
	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	public String deleteExperiment(@RequestParam(value="id") Long id,SessionStatus sessionStatus, Model model,HttpServletRequest request, HttpServletResponse response) {
		InteractiveExperiment exp = intExperimentRepository.findById(id);
		SimulationsManager sManager = exp.getSimulationsManager();
		sManager.setSize();
		if(sManager.getSize()==1){
			simulationsManagerRepository.delete(exp.getSimulationsManager());
		}else{
			intExperimentRepository.delete(exp); 
			exp.getSimulationsManager().refreshState();
			ds.updateExp(exp);
			ds.updateManager(sManager);
		}
		return "redirect:" + request.getHeader("Referer");
	}
}