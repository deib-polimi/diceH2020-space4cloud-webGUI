package it.polimi.diceH2020.launcher.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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
		String fileNamePrefix = generateUniqueString();
		int j = 1;
		
		/*
		if(hasDuplicate(Arrays.stream(files).map(f-> f.getOriginalFilename()).collect(Collectors.toList()))){
			model.addAttribute("message", "Duplicated files!");
			return "fileUpload";
		}
		*/
		
		for (int i = 0; i < files.length; i++) {
			fileName = fileNamePrefix + files[i].getOriginalFilename().replaceAll("/", "");
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
		String fileNamePrefix = generateUniqueString();
		int j = 1;
		
		/*
		if(hasDuplicate(Arrays.stream(files).map(f-> f.getOriginalFilename()).collect(Collectors.toList()))){
			model.addAttribute("message", "Duplicated files!");
			return "fileUpload";
		}
		*/
		
		for (int i = 0; i < files.length; i++) {
			fileName = fileNamePrefix + files[i].getOriginalFilename().replaceAll("/", "");
			File f = saveFile(files[i], fileName);
			if (f == null) return "error";
			if (fileName.contains(".json")) {
				if (!validator.validateOptInput(f.toPath())) {
					model.addAttribute("message", "Invalid Json file!");
					return "fileUploadOpt";
				} else {
					redirectAttrs.addAttribute("inputPath", f.toPath().toString());
				}
			} else {
				redirectAttrs.addAttribute("pathFile"+j, f.toPath().toString());
				j++;
			}
		}
		return "redirect:/launch/opt/simulationSetup";
	}

	public static <T> boolean hasDuplicate(Iterable<T> all) {
	    Set<T> set = new HashSet<T>();
	    // Set#add returns false if the set does not change, which
	    // indicates that a duplicate element has been added.
	    for (T each: all) if (!set.add(each)) return true;
	    return false;
	}
	
	private String generateUniqueString() {
		//String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		Date dNow = new Date( );
	    SimpleDateFormat ft = new SimpleDateFormat ("Edd-MM-yyyy_HH-mm-ss");
	    Random random = new Random();
	    String id = ft.format(dNow)+random.nextInt(99999);
	    return id;
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
