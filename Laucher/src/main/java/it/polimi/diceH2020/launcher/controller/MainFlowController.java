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
import org.springframework.web.bind.annotation.SessionAttributes;

import it.polimi.diceH2020.launcher.FileService;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.ExperimentRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsRepository;


@SessionAttributes("sim_manager") //it will persist in each browser tab, resolved with http://stackoverflow.com/questions/368653/how-to-differ-sessions-in-browser-tabs/11783754#11783754
@Controller
public class MainFlowController {
	@Autowired
	private FileService fileService;
	
	@Autowired
	private SimulationsManagerRepository simulationsManagerRepository;
	@Autowired
	private SimulationsRepository simulationsRepository;
	@Autowired
	private ExperimentRepository expRepository;
	
	
	@RequestMapping(value="/", method=RequestMethod.GET)
    public String showHome(){ 
    	return "home";
    }
	@RequestMapping(value="/list/instances", method=RequestMethod.GET)
	public String listSolutions(Model model) {
			model.addAttribute("sim_manager", fileService.getListFileWithIndex());
			return "solutionList";
	}
	
	
	@RequestMapping(value="/list", method=RequestMethod.GET)
	public String list2(Model model) {
			model.addAttribute("sim_manager", simulationsManagerRepository.findAll());
			return "simManagersList";
	}
	
	@RequestMapping(value="/list2", method=RequestMethod.GET)
	public String list3(Model model) {
			model.addAttribute("sim", simulationsRepository.findAll());
			return "simList";
	}
	
	@RequestMapping(value="/results", method=RequestMethod.GET)
	public String results(@RequestParam("id") Long id,Model model) {
			SimulationsManager sim_manager = simulationsManagerRepository.findOne(id);
			model.addAttribute("sim", simulationsRepository.findBySimulationsManager(sim_manager));
			return "simList";
	}
	
	
	@RequestMapping(value="/listV7", method=RequestMethod.GET)
	public String listV7(Model model) {
			model.addAttribute("sim_manager", simulationsManagerRepository.findByModel("V7"));
			return "simManagersList";
	}
	
	@RequestMapping(value="/listV10", method=RequestMethod.GET)
	public String listV10(Model model) {			model.addAttribute("sim_manager", simulationsManagerRepository.findByModel("V10"));
			return "simManagersList";
	}
	
	
	@RequestMapping(value="/download", method=RequestMethod.GET)
	@ResponseBody FileSystemResource downloadFile(@RequestParam(value="id") Long id,HttpServletResponse response) {
	    SimulationsManager manager = simulationsManagerRepository.findOne(id);
	    response.setContentType("application/ms-excel");
	    response.setHeader( "Content-Disposition", "attachment;filename=" + manager.getFolderPath()+"results.xls" );
	    return new FileSystemResource(new File(manager.getFolderPath()+"results.xls"));
	}
	
}
