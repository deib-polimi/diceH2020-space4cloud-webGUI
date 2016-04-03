package it.polimi.diceH2020.launcher.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
	
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DiceService.class.getName());
	
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
	public String resultsWI(@RequestParam("id") Long id,Model model,@ModelAttribute("message") String message) {
			SimulationsManager simManager = simulationsManagerRepository.findOne(id);
			model.addAttribute("sim", intExperimentRepository.findBySimulationsManager(simManager));
			return "simWIList";
	}
	
	@RequestMapping(value="/resultsOpt", method=RequestMethod.GET)
	public String resultsOpt(@RequestParam("id") Long id,Model model,@ModelAttribute("message") String message) {
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
	public synchronized String relaunchExperiment (@RequestParam(value="id") Long id,SessionStatus sessionStatus, Model model,HttpServletRequest request,RedirectAttributes redirectAttrs) {
		String idFrom, folder;
		idFrom= folder = new String();
		try{
			InteractiveExperiment exp = intExperimentRepository.findById(id);
			exp.setExperimentalDuration(0);
			exp.setResponseTime(0d);
			exp.setFinalSolution(new String()); //inverse of e.setSol(sol);
			String type = new String();
			SimulationsManager simManager = exp.getSimulationsManager();
			idFrom = simManager.getId().toString();
			folder = simManager.getFolder(); 
			if(exp.getState().equals("completed")){
				exp.setDone(false);
				exp.setState("ready");
				exp.setNumSolutions(exp.getNumSolutions()-1); 
				type = simManager.getType();
				simManager.setNumCompletedSimulations(simManager.getNumCompletedSimulations()-1);
				if(simManager.getState().equals("completed")){
					simManager.setState("ready"); //TODO ready?running?partiallyCompleted?
				}
			}
			ds.simulation(exp);
			if(type.equals("WI")){
				return "redirect:/resultsWI?id="+idFrom;
			}else{
				return "redirect:/simOptByFolder?folder="+folder;
			}
		}catch(Exception e){
			redirectAttrs.addAttribute("message", "Error trying to relaunch an experiment.");
			logger.info("Error trying to relaunch an experiment.");
			return "redirect:" + request.getHeader("Referer");
		}
	}
	
	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	public synchronized String deleteExperiment(@RequestParam(value="id") Long id,SessionStatus sessionStatus, Model model,HttpServletRequest request,RedirectAttributes redirectAttrs) {
		String idFrom, folder;
		idFrom= folder = new String();
		try{
			InteractiveExperiment exp = intExperimentRepository.findById(id);
			SimulationsManager sManager = simulationsManagerRepository.findById(exp.getSimulationsManager().getId());
			idFrom = sManager.getId().toString();
			folder = sManager.getFolder(); 
			String type = sManager.getType();
			sManager.setSize();
			if(sManager.getSize()==1){
				System.out.println("deleted manager"+sManager.getId());
				simulationsManagerRepository.delete(sManager);
			}else{
				sManager.getExperimentsList().remove(exp);
				sManager.refreshState();
				sManager.setSize();
				ds.updateManager(sManager);//intExperimentRepository.delete(exp); done by orphandelete=true 
			}
			
			if(type.equals("WI")){
				return "redirect:/resultsWI?id="+idFrom;
			}else{
				return "redirect:/simOptByFolder?folder="+folder;
			}
		}catch(Exception e){
			redirectAttrs.addAttribute("message", "Error trying to delete an experiment.");
			logger.info("Error trying to delete an experiment.");
			return "redirect:" + request.getHeader("Referer");
		}
	}
}