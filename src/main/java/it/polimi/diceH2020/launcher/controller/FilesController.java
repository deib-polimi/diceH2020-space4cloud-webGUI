package it.polimi.diceH2020.launcher.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.launcher.service.Validator;
import it.polimi.diceH2020.launcher.utility.FileUtility;


@Controller
@RequestMapping("/files")
public class FilesController {

	@Autowired
	FileUtility fileUtility;
	@Autowired
	Validator validator;

	@RequestMapping(value = "/upload", method = RequestMethod.GET)
	public @ResponseBody String hello() {
		return "done";
	}

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public String multipleSave(@RequestParam("file") MultipartFile[] files, @RequestParam("scenario") String useCase, Model model, RedirectAttributes redirectAttrs) {
		String fileName = null;
		int j = 1;
		boolean singleInputData = false;
		Scenarios scenario = Scenarios.valueOf(useCase);
		
		redirectAttrs.addAttribute("scenario", scenario);
		model.addAttribute("scenario", scenario);
		
		for (int i = 0; i < files.length; i++) {
			fileName = files[i].getOriginalFilename().replaceAll("/", "");
			File f = saveFile(files[i], fileName);
			if (f == null) return "error";
			if (fileName.contains(".json")) {
				    if(validator.validateInstanceDataMultiProvider(f.toPath())){
				    	redirectAttrs.addAttribute("instanceDataMultiProvider", f.toPath().toString());
				    	continue;
				    }
				    else if(validator.validateInstanceData(f.toPath())){
				    		redirectAttrs.addAttribute("instanceData", f.toPath().toString());
				    		singleInputData = true;
					    	continue;
				    	 }
					model.addAttribute("message", "You have submitted an invalid json!");
					return "launchSimulation_FileUpload";
			} else {
				redirectAttrs.addAttribute("pathFile"+j, f.toPath().toString());
				j++;
			}
		}
		if(singleInputData) return "redirect:/launch/simulationSetupSingleInputData";
		return "redirect:/launch/simulationSetup";
	}
	
	//Deprecated, method used to upload .json single parameters
	@RequestMapping(value = "/uploadWithSeparateJson", method = RequestMethod.POST)
	public String multipleSave2(@RequestParam("file") MultipartFile[] files, @RequestParam("scenario") String scenario, Model model, RedirectAttributes redirectAttrs) {
		String fileName = null;
		int j = 1;
		
		redirectAttrs.addAttribute("scenario", Scenarios.valueOf(scenario));
		model.addAttribute("scenario", Scenarios.valueOf(scenario));
		
		for (int i = 0; i < files.length; i++) {
			fileName = files[i].getOriginalFilename().replaceAll("/", "");
			File f = saveFile(files[i], fileName);
			if (f == null) return "error";
			if (fileName.contains(".json")) {
				    if(validator.validateClassParameters(f.toPath())){
				    	redirectAttrs.addAttribute("classParametersPath", f.toPath().toString());
				    	continue;
				    }
				    if(validator.validateJobProfile(f.toPath())){
				    	redirectAttrs.addAttribute("jobProfilePath", f.toPath().toString());
				    	continue;
				    }
				    if(validator.validatePrivateCloudParameters(f.toPath())){
				    	redirectAttrs.addAttribute("privateCloudParametersPath", f.toPath().toString());
				    	continue;
				    }
				    if(validator.validatePublicCloudParameters(f.toPath())){
				    	redirectAttrs.addAttribute("publicCloudParametersPath", f.toPath().toString());
				    	continue;
				    }
				    if(validator.validateVMConfigurations(f.toPath())){
				    	redirectAttrs.addAttribute("vmConfigurationsPath", f.toPath().toString());
				    	continue;
				    }
					model.addAttribute("message", fileName+" isn't valid!");
					return "launchSimulation_FileUpload";
			} else {
				redirectAttrs.addAttribute("pathFile"+j, f.toPath().toString());
				j++;
			}
		}
		
		return "redirect:/launch/simulationSetupWithMultipleJson";
	}
	
	private File saveFile(MultipartFile file, String fileName) {
		try {
			byte[] bytes = file.getBytes();
			File f = fileUtility.provideFile(fileName);
			BufferedOutputStream buffStream = new BufferedOutputStream(new FileOutputStream(f));
			buffStream.write(bytes);
			buffStream.close();
			return f;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
