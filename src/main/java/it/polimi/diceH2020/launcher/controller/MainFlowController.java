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

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.launcher.FileService;
import it.polimi.diceH2020.launcher.States;
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
		model.addAttribute("wsStatusMap", ds.getWsStatus());
		model.addAttribute("queueSize", ds.getQueueSize());
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
		model.addAttribute("cloudTypes", CloudType.values());
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
			model.addAttribute("sim", intExperimentRepository.findBySimulationsManagerOrderByIdAsc(simManager));
			return "simWIList";
	}
	
	@RequestMapping(value="/resultsOpt", method=RequestMethod.GET)
	public String resultsOpt(@RequestParam("id") Long id,Model model,@ModelAttribute("message") String message) {
			SimulationsManager simManager = simulationsManagerRepository.findOne(id);
			model.addAttribute("sim", intExperimentRepository.findBySimulationsManagerOrderByIdAsc(simManager));
			return "simOptList";
	}
	
	@RequestMapping(value="/foldersOpt", method=RequestMethod.GET)
	public String foldersOpt(Model model) {
			model.addAttribute("folders", simulationsManagerRepository.findByIdInOrderByIdAsc(simulationsManagerRepository.findSimManagerGroupedByFolders()));
			return "foldersOfSimOptMan";
	}
	
	@RequestMapping(value="/resWI", method=RequestMethod.GET)
	public String listWi(Model model) {
			model.addAttribute("sim_manager", simulationsManagerRepository.findByTypeOrderByIdAsc("WI"));
			return "simManagersWIList";
	}
	
	@RequestMapping(value="/resOpt", method=RequestMethod.GET)
	public String listOpt(Model model) {
			model.addAttribute("sim_manager", simulationsManagerRepository.findByTypeOrderByIdAsc("Opt"));
			return "simManagersOptList";
	}
	
	@RequestMapping(value="/simOptByFolder", method=RequestMethod.GET)
	public String simOptFolderContent(@RequestParam(value="folder") String folder, Model model) {
	    model.addAttribute("sim_manager", simulationsManagerRepository.findByFolderOrderByIdAsc(folder));
	    return "simManagersOptList";
	}
	
	@RequestMapping(value = "/relaunch", method = RequestMethod.GET)
	public synchronized String relaunchExperiment (@RequestParam(value="id") Long id,SessionStatus sessionStatus, Model model,HttpServletRequest request,RedirectAttributes redirectAttrs) {
		String idFrom, folder;
		idFrom= folder = new String();
		InteractiveExperiment exp = intExperimentRepository.findById(id);
		if(exp.getState().equals(States.COMPLETED)||exp.getState().equals(States.ERROR)){
			try{
				exp.setExperimentalDuration(0);
				exp.setResponseTime("");
				exp.setFinalSolution(new String()); //inverse of e.setSol(sol);
				exp.setDone(false);
				String type = new String();
				SimulationsManager simManager = exp.getSimulationsManager();
				idFrom = simManager.getId().toString();
				folder = simManager.getFolder(); 
				type = simManager.getType();
				
				if(exp.getState().equals(States.COMPLETED)){
					exp.setNumSolutions(exp.getNumSolutions()-1); 
					simManager.setNumCompletedSimulations(simManager.getNumCompletedSimulations()-1);
				}
				if(exp.getState().equals(States.ERROR)){
					simManager.setNumFailedSimulations(simManager.getNumFailedSimulations()-1);
				}
				exp.setState(States.READY);
				simManager.refreshState();
				ds.updateManager(simManager);
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
		redirectAttrs.addAttribute("message", "Cannot relaunch an uncompleted experiment.");
		return "redirect:" + request.getHeader("Referer");
	}
	
	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	public synchronized String deleteExperiment(@RequestParam(value="id") Long id,SessionStatus sessionStatus, Model model,HttpServletRequest request,RedirectAttributes redirectAttrs) {
		String idFrom, folder;
		idFrom= folder = new String();
		InteractiveExperiment exp = intExperimentRepository.findById(id);
		if(exp.getState().equals(States.COMPLETED)||exp.getState().equals(States.ERROR)){
			try{
				
				SimulationsManager sManager = simulationsManagerRepository.findById(exp.getSimulationsManager().getId());
				idFrom = sManager.getId().toString();
				folder = sManager.getFolder(); 
				String type = sManager.getType();
				//sManager.setSize();
				if(sManager.getSize()==1){
					System.out.println("deleted manager"+sManager.getId());
					simulationsManagerRepository.delete(sManager);
					return "redirect:/";
				}else{
					sManager.getExperimentsList().remove(exp);
					sManager.refreshState();
					//sManager.setSize();
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
		redirectAttrs.addAttribute("message", "Cannot delete an uncompleted experiment .");
		return "redirect:" + request.getHeader("Referer");
	}
}