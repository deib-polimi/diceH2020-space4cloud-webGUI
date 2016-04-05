package it.polimi.diceH2020.launcher.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

	@RequestMapping(value = "/uploadWI", method = RequestMethod.POST)
	public String multipleSaveWI(@RequestParam("file") MultipartFile[] files, Model model, RedirectAttributes redirectAttrs) {
		// TODO remove all files
		String fileName = null;
		int j = 1;
		
		for (int i = 0; i < files.length; i++) {
			fileName = files[i].getOriginalFilename().replaceAll("/", "");
			File f = saveFile(files[i], fileName);
			if (f == null) return "error";
			if (fileName.contains(".json")) {
				if (!validator.validateWISolution(f.toPath())) {
					model.addAttribute("message", "Invalid Json file!");
					return "fileUploadWI";
				} else {
					redirectAttrs.addAttribute("inputPath", f.toPath().toString());
				}
			} else {
				redirectAttrs.addAttribute("pathFile"+j, f.toPath().toString());
				j++;
			}
		}
		return "redirect:/launch/wi/simulationSetup";
	}
	
	@RequestMapping(value = "/uploadOpt", method = RequestMethod.POST)
	public String multipleSaveOpt(@RequestParam("file") MultipartFile[] files, Model model, RedirectAttributes redirectAttrs) {
		// TODO remove all files
		String fileName = null;
		ArrayList<ArrayList<String>> tmpValues = new ArrayList<ArrayList<String>>();
		if(hasDuplicate(Arrays.stream(files).map(f-> f.getOriginalFilename()).collect(Collectors.toList()))){
			model.addAttribute("message", "Duplicated files!");
			return "fileUploadOpt";
		}
		
		for (int i = 0; i < files.length; i++) {
			fileName = files[i].getOriginalFilename().replaceAll("/", "");
			
			System.out.println(fileName);
			
			File f = saveFile(files[i], fileName);
			if (f == null) return "error";
			if (fileName.contains(".json")) {
				if (!validator.validateOptInput(f.toPath())) {
					model.addAttribute("message", "Invalid Json file!");
					return "fileUploadOpt";
				} else {
					ArrayList<String> tmp = new ArrayList<String>();
					tmp.add(f.toPath().toString());
					tmpValues.add(0,tmp);
				}
			} else {
				tmpValues.get(0).add(f.toPath().toString());
			}
		}
		redirectAttrs.addFlashAttribute("pathList", tmpValues);
		return "redirect:/launch/opt/simulationSetup";
	}
	
	//assuming that the order in folder is: .JSON1 json1map1.txt json1map2.txt json1rs.txt  json1rs1.txt JSON2 json2map1.txt ...
	@RequestMapping(value = "/uploadOptFolderFiles", method = RequestMethod.POST)
	public String multipleSaveOptFromFolder(@RequestParam("file[]") MultipartFile[] files, Model model, RedirectAttributes redirectAttrs) {
		// TODO remove all files
		String fileName = null;
		//int j = 1;
		int k = 0;
		
		if(files == null || files.length == 0){
			model.addAttribute("message", "Wrong files!");
			return "fileUploadOpt";
		}
		
		if(hasDuplicate(Arrays.stream(files).map(f-> f.getOriginalFilename()).collect(Collectors.toList()))){
			model.addAttribute("message", "Duplicated files!");
			return "fileUploadOpt";
		}
		
		ArrayList<ArrayList<String>> tmpValues = new ArrayList<ArrayList<String>>();
		boolean firstEl = true;
		for (int i = 0; i < files.length; i++) {
			String[] splits = files[i].getOriginalFilename().split("/");
			fileName = splits[splits.length-1];
			System.out.println(fileName);
			
			if (fileName.contains(".json")) {
				File f = saveFile(files[i], fileName);
				if (f == null) return "error";
				if (!validator.validateOptInput(f.toPath())) {
					model.addAttribute("message", "Invalid Json file!");
					return "fileUploadOpt";
				} else {
					//redirectAttrs.addAttribute("inputPath"+(2-j+i), f.toPath().toString());
					if(!firstEl){
						k++;
					}else{
						firstEl = false;
					}
					ArrayList<String> tmp = new ArrayList<String>();
					tmp.add(f.toPath().toString());
					tmpValues.add(k,tmp);
				}
			} else if(fileName.contains(".txt")){
				File f = saveFile(files[i], fileName);
				if (f == null) return "error";
				//redirectAttrs.addAttribute("pathFile"+j, f.toPath().toString());
				tmpValues.get(k).add(f.toPath().toString());
				//j++;
			}
		}
		redirectAttrs.addFlashAttribute("pathList", tmpValues);
		return "redirect:/launch/opt/simulationSetup";
	}

	public static <T> boolean hasDuplicate(Iterable<T> all) {
	    Set<T> set = new HashSet<T>();
	    // Set#add returns false if the set does not change, which
	    // indicates that a duplicate element has been added.
	    for (T each: all) if (!set.add(each)) return true;
	    return false;
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
