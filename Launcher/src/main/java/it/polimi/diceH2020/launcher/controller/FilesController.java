package it.polimi.diceH2020.launcher.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
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
	public String multipleSave(@RequestParam("file") MultipartFile[] files, Model model, RedirectAttributes redirectAttrs) {
		// TODO remove all files
		String fileName = null;
		int j = 1;
		
		if(hasDuplicate(Arrays.stream(files).map(f-> f.getOriginalFilename()).collect(Collectors.toList()))){
			model.addAttribute("message", "Duplicated files!");
			return "home";
		}
		
		for (int i = 0; i < files.length; i++) {
			fileName = files[i].getOriginalFilename();
			File f = saveFile(files[i]);
			if (f == null) return "error";
			if (fileName.contains(".json")) {
				if (!validator.validateSolution(f.toPath())) {
					model.addAttribute("message", "Invalid Json file!");
					return "home";
				} else {
					//
					redirectAttrs.addAttribute("inputPath", f.toPath().toString());
				}
			} else {
				redirectAttrs.addAttribute("pathFile"+j, f.toPath().toString());
				j++;
			}
		}
		return "redirect:/sim/simulationSetup";
	}


	public static <T> boolean hasDuplicate(Iterable<T> all) {
	    Set<T> set = new HashSet<T>();
	    // Set#add returns false if the set does not change, which
	    // indicates that a duplicate element has been added.
	    for (T each: all) if (!set.add(each)) return true;
	    return false;
	}
	
	
	private File saveFile(MultipartFile file) {
		String fileName = file.getOriginalFilename();
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
