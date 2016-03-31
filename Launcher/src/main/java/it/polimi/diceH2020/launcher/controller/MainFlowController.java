package it.polimi.diceH2020.launcher.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
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
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.model.SimulationsWIManager;
import it.polimi.diceH2020.launcher.repository.InteractiveExperimentRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import it.polimi.diceH2020.launcher.utility.Compressor;
import it.polimi.diceH2020.launcher.utility.ExcelWriter;
import it.polimi.diceH2020.launcher.utility.FileUtility;


//@SessionAttributes("sim_manager") //it will persist in each browser tab, resolved with http://stackoverflow.com/questions/368653/how-to-differ-sessions-in-browser-tabs/11783754#11783754
@Controller
public class MainFlowController {
	@Autowired
	private FileService fileService;
	
	private static Logger logger = Logger.getLogger(FileUtility.class.getName());

	@Autowired
	private SimulationsManagerRepository simulationsManagerRepository;
	
	@Autowired
	private InteractiveExperimentRepository intExperimentRepository;
	
	@Autowired
	private ExcelWriter excelWriter;
	
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
	@RequestMapping(value="/downloadPartial", method=RequestMethod.GET)
	@ResponseBody void downloadPartialExcel(@RequestParam(value="id") Long id,HttpServletResponse response) {
	    SimulationsWIManager manager = (SimulationsWIManager)simulationsManagerRepository.findOne(id);
	    Workbook wb = excelWriter.createWorkbook(manager);
	    try {
			wb.write(response.getOutputStream());
			 //response.setContentType("application/ms-excel;charset=utf-8");
		    response.setContentType("application/vnd.ms-excel;charset=utf-8");
		    //response.setContentType(new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
		    response.setHeader( "Content-Disposition", "attachment;filename = results.xls" );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value="/downloadJson", method=RequestMethod.GET)
	@ResponseBody String downloadWIJson(@RequestParam(value="id") Long id,HttpServletResponse response) throws JsonProcessingException, IOException {
	    SimulationsManager manager = simulationsManagerRepository.findOne(id);
	    response.setContentType("application/json");
	    response.setHeader( "Content-Disposition", "attachment;filename = " + manager.getInstanceName() + ".json" );
	    return Compressor.decompress(manager.getInput());
	}
	
	@RequestMapping(value="/downloadFinalJson", method=RequestMethod.GET)
	@ResponseBody String downloadFinalSolOptJson(@RequestParam(value="id") Long id,HttpServletResponse response) throws JsonProcessingException, IOException {
		InteractiveExperiment exp = intExperimentRepository.findOne(id);
	    response.setContentType("application/json");
	    response.setHeader( "Content-Disposition", "attachment;filename = " + exp.getInstanceName()+ "SOL.json" );
	    return Compressor.decompress(exp.getFinalSolution());
	}
	
	@RequestMapping(value="/downloadTxt", method=RequestMethod.GET)
	@ResponseBody String downloadTxt(@RequestParam(value="id") Long id, @RequestParam(value="txt") int num,HttpServletResponse response) throws JsonProcessingException, IOException {
	    SimulationsManager manager = simulationsManagerRepository.findOne(id);
	    response.setContentType("text/plain;charset=utf-8");
	   
	    if(num==2){
	    	response.setHeader( "Content-Disposition", "attachment;filename = " + manager.getInputFile(0, 1) + ".txt" );
	    	return manager.getInputFile(0, 3);
	    }else{
	    	response.setHeader( "Content-Disposition", "attachment;filename = " + manager.getInputFile(0, 0) + ".txt" );
	    	return manager.getInputFile(0, 2);
	    }
	}
	
	@RequestMapping(value="/downloadZipOptSim", method=RequestMethod.GET)
	@ResponseBody void downloadZipOptSimManInputs(@RequestParam(value="id") Long id,HttpServletResponse response) {
	    SimulationsManager manager = simulationsManagerRepository.findOne(id);
	    ArrayList<String[]> inputFiles = manager.getInputFiles();
	    
	    Map<String, String> files = new HashMap<String,String>();
	    files.put(manager.getInputFileName(),manager.getInput() );
	    
	    for(int i=0; i<inputFiles.size();i++){
	    	files.put(inputFiles.get(i)[0],inputFiles.get(i)[2]);
	    	files.put(inputFiles.get(i)[1],inputFiles.get(i)[3]);
	    }
	    String zipPath = new String();
		
	    zipPath = FileUtility.createTempZip(files);
	    if(!Files.exists(Paths.get(zipPath))){
	    	System.out.println("Problem creatin zip file");
	    }
	    response.setContentType("application/zip");
	    response.addHeader("Content-Disposition", "attachment; filename=\"test.zip\"");
	    try{
		    InputStream is = new FileInputStream(zipPath);
		    IOUtils.copy(is, response.getOutputStream());
		    response.flushBuffer();
	    }catch(Exception e){
	    	logger.info("Impossible returning zip file as an HTTP response");
	    }
	}
	@RequestMapping(value="/downloadZipOptInputFolder", method=RequestMethod.GET)
	@ResponseBody void downloadZipOptInputFolder(@RequestParam(value="folder") String folder,HttpServletResponse response) {
		List<SimulationsManager> folderManagerList =  simulationsManagerRepository.findByFolder(folder);
		Map<String, String> files = new HashMap<String,String>();
		String zipPath = new String();
		
		for(int managerEntry=0;managerEntry<folderManagerList.size();managerEntry++){
		    SimulationsManager manager = folderManagerList.get(managerEntry);
		    ArrayList<String[]> inputFiles = manager.getInputFiles();
		    files.put(manager.getInputFileName(),manager.getInput() );
		    
		    for(int i=0; i<inputFiles.size();i++){
		    	files.put(inputFiles.get(i)[0],inputFiles.get(i)[2]);
		    	files.put(inputFiles.get(i)[1],inputFiles.get(i)[3]);
		    }
		    
		}
	    zipPath = FileUtility.createTempZip(files);
	    if(!Files.exists(Paths.get(zipPath))){
	    	System.out.println("Problem creatin zip file");
	    }
	    response.setContentType("application/zip");
	    response.addHeader("Content-Disposition", "attachment; filename=\"test.zip\"");
	    try{
		    InputStream is = new FileInputStream(zipPath);
		    IOUtils.copy(is, response.getOutputStream());
		    response.flushBuffer();
	    }catch(Exception e){
	    	logger.info("Impossible returning zip file as an HTTP response");
	    }
	}
	@RequestMapping(value="/downloadZipOptOutputJsons", method=RequestMethod.GET)
	@ResponseBody void downloadZipOptOutputJsons(@RequestParam(value="folder") String folder,HttpServletResponse response) {
		List<SimulationsManager> folderManagerList =  simulationsManagerRepository.findByFolder(folder);
		Map<String, String> files = new HashMap<String,String>();
		String zipPath = new String();
		
		for(int managerEntry=0;managerEntry<folderManagerList.size();managerEntry++){
		    SimulationsManager manager = folderManagerList.get(managerEntry);
		    List<InteractiveExperiment> intExpList = manager.getExperimentsList();
		    for(int i=0; i<intExpList.size();i++){
		    	if(intExpList.get(i)!=null){
			    	if(intExpList.get(i).getState().equals("completed")){
			    		files.put(intExpList.get(i).getInstanceName()+".json",intExpList.get(i).getFinalSolution() );
			    		System.out.println("Created "+intExpList.get(i).getInstanceName()+".json");
			    	}
		    	}
		    }
		}
	    zipPath = FileUtility.createTempZip(files);
	    if(!Files.exists(Paths.get(zipPath))){
	    	System.out.println("Problem creatin zip file");
	    }
	    response.setContentType("application/zip");
	    response.addHeader("Content-Disposition", "attachment; filename=\"test.zip\"");
	    try{
		    InputStream is = new FileInputStream(zipPath);
		    IOUtils.copy(is, response.getOutputStream());
		    response.flushBuffer();
	    }catch(Exception e){
	    	logger.info("Impossible returning zip file as an HTTP response");
	    }
	}
	
	
}