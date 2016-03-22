package it.polimi.diceH2020.launcher.controller;

import java.io.File;
import java.io.IOException;

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

import com.fasterxml.jackson.core.JsonProcessingException;

import it.polimi.diceH2020.launcher.FileService;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.InteractiveExperimentRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import it.polimi.diceH2020.launcher.utility.Compressor;


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
	
	@RequestMapping(value="/download", method=RequestMethod.GET)
	@ResponseBody FileSystemResource downloadExcel(@RequestParam(value="id") Long id,HttpServletResponse response) {
	    SimulationsManager manager = simulationsManagerRepository.findOne(id);
	    //response.setContentType("application/ms-excel;charset=utf-8");
	    response.setContentType("application/vnd.ms-excel;charset=utf-8");
	    //response.setContentType(new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	    //response.setHeader( "Content-Disposition", "inline;filename = results.xlsx" );
	    response.setHeader( "Content-Disposition", "attachment;filename = results.xls" );
	    return new FileSystemResource(new File(manager.getResultFilePath()));
	}
	
	@RequestMapping(value="/downloadJson", method=RequestMethod.GET)
	@ResponseBody String downloadWIJson(@RequestParam(value="id") Long id,HttpServletResponse response) throws JsonProcessingException, IOException {
	    SimulationsManager manager = simulationsManagerRepository.findOne(id);
	    response.setContentType("application/json");
	    response.setHeader( "Content-Disposition", "attachment;filename = " + manager.getInstanceName() + ".json" );
	    return Compressor.decompress(manager.getInput());
	}
	
	@RequestMapping(value="/downloadTxt", method=RequestMethod.GET)
	@ResponseBody String downloadTxt(@RequestParam(value="id") Long id, @RequestParam(value="txt") int num,HttpServletResponse response) throws JsonProcessingException, IOException {
	    SimulationsManager manager = simulationsManagerRepository.findOne(id);
	    response.setContentType("text/plain;charset=utf-8");
	   
	    if(num==2){
	    	response.setHeader( "Content-Disposition", "attachment;filename = " + manager.getRsFileName() + ".txt" );
	    	return manager.getRsFile();
	    }else{
	    	response.setHeader( "Content-Disposition", "attachment;filename = " + manager.getMapFileName() + ".txt" );
	    	return manager.getMapFile();
	    }
	}
}