package it.polimi.diceH2020.launcher.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

	private List<String> saveJsonFile(MultipartFile[] files) {
		String fileName;
		for (int i = 0; i < files.length; i++) {
			try {
				fileName = files[i].getOriginalFilename();
				if (fileName.contains(".json")) {
					File f = saveFile(files[i]);
					if (!validator.validateSolution(f.toPath())) return null;
					else {
						Solution sol = validator.objectFromPath(f.toPath(), Solution.class).get();
						List<String> lst = new ArrayList<>(3);
						lst.add(sol.getId());
						SolutionPerJob spj = sol.getLstSolutions().get(0);
						lst.add(Integer.toString(spj.getJob().getId()));
						lst.add(spj.getTypeVMselected().getId());
						return lst;
					}
				}
			} catch (Exception e) {
				return null;
			}
		}
		return null;
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
