package it.polimi.diceH2020.launcher.controller;

import java.io.File;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.SessionStatus;

import it.polimi.diceH2020.launcher.FileService;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.InteractiveExperimentRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;


//@SessionAttributes("sim_manager") //it will persist in each browser tab, resolved with http://stackoverflow.com/questions/368653/how-to-differ-sessions-in-browser-tabs/11783754#11783754
@Controller
public class MainFlowController {
	@Autowired
	private FileService fileService;
	
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
	@RequestMapping(value="/list/instances", method=RequestMethod.GET)
	public String listSolutions(Model model) {
			model.addAttribute("sim_manager", fileService.getListFileWithIndex());
			return "solutionList";
	}
	
	
	@RequestMapping(value="/list", method=RequestMethod.GET)
	public String listExperiments(Model model) {
			model.addAttribute("sim_manager", simulationsManagerRepository.findAll());
			return "simManagersList";
	}
	
	@RequestMapping(value="/results", method=RequestMethod.GET)
	public String results(@RequestParam("id") Long id,Model model) {
			SimulationsManager simManager = simulationsManagerRepository.findOne(id);
			model.addAttribute("sim", intExperimentRepository.findBySimulationsManager(simManager));
			return "simList";
	}
	
	
	@RequestMapping(value="/totalExperimentList", method=RequestMethod.GET)
	public String listV7(Model model) {
			model.addAttribute("sim_manager", simulationsManagerRepository.findAll());
			return "simManagersList";
	}
	
	
	@RequestMapping(value="/download", method=RequestMethod.GET)
	@ResponseBody FileSystemResource downloadFile(@RequestParam(value="id") Long id,HttpServletResponse response) {
	    SimulationsManager manager = simulationsManagerRepository.findOne(id);
	    response.setContentType("application/ms-excel");
	    response.setHeader( "Content-Disposition", "attachment;filename = results.xls" );
	    return new FileSystemResource(new File(manager.getResultFilePath()));
	}
	
}
