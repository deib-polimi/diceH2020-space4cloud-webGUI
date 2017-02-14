/*
Copyright 2016-2017 Eugenio Gianniti
Copyright 2016 Jacopo Rigoli

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.launcher.controller.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.launcher.FileService;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.InteractiveExperimentRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import it.polimi.diceH2020.launcher.utility.Compressor;
import it.polimi.diceH2020.launcher.utility.ExcelWriter;
import it.polimi.diceH2020.launcher.utility.FileUtility;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DownloadsController {

	private final Logger logger = Logger.getLogger(getClass ());

	@Autowired
	private SimulationsManagerRepository simulationsManagerRepository;

	@Autowired
	private InteractiveExperimentRepository intExperimentRepository;

	@Autowired
	private ExcelWriter excelWriter;

	@Autowired
	private FileUtility fileUtility;

	@Autowired
	private FileService fileService;

	@RequestMapping(value="/download", method=RequestMethod.GET)
	@ResponseBody void downloadPartialExcel(@RequestParam(value="id") Long id, HttpServletResponse response) {
		SimulationsManager manager = simulationsManagerRepository.findOne(id);
		Workbook wb = excelWriter.createWorkbook(manager);
		try {
			response.setContentType("application/vnd.ms-excel;charset=utf-8");
			response.setHeader( "Content-Disposition", "attachment;filename = results.xls" );
			wb.write(response.getOutputStream());
			response.flushBuffer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Inputs/Folder/(1 JSON, n TXT)
	 * Results/Folder/SimManager(for each provider)/(1 JSON)
	 *
	 */
	@RequestMapping(value="/downloadAll", method=RequestMethod.GET)
	@ResponseBody void downloadAll(@RequestParam(value="type") String type, HttpServletResponse response) {
		List<SimulationsManager> smList;

		switch(CloudType.valueOf(type)){
			case Private:
				smList = simulationsManagerRepository.findByIdInOrderByIdAsc(
						simulationsManagerRepository.findPrivateSimManGroupedByFolders(
								Scenarios.PrivateAdmissionControl, Scenarios.PrivateNoAdmissionControl,
								Scenarios.PrivateAdmissionControlWithPhysicalAssignment));
				break;
			case Public:
				smList = simulationsManagerRepository.findByIdInOrderByIdAsc(
						simulationsManagerRepository.findPublicSimManGroupedByFolders(
								Scenarios.PublicAvgWorkLoad, Scenarios.PublicPeakWorkload,
								Scenarios.StormPublicAvgWorkLoad));
				break;
			default:
				return;
		}

		downloadSM(smList,response);
	}

	/**
	 * Inputs/Folder/(1 JSON, n TXT)
	 * Results/Folder/SimManager(for each provider)/(1 JSON)
	 *
	 */
	@RequestMapping(value="/downloadSelected", method=RequestMethod.GET)
	@ResponseBody void downloadSelected(@RequestParam(value="ids[]") String ids, HttpServletResponse response) {
		List<SimulationsManager> smList = new ArrayList<>();

		String[] parts = ids.split(",");
		for(String folderID : parts){
			smList.addAll(simulationsManagerRepository.findByFolderOrderByIdAsc(folderID));
		}

		downloadSM(smList,response);
	}

	private void downloadSM(List<SimulationsManager> smList, HttpServletResponse response) {
		Map<String,String> files = new HashMap<>();

		for (SimulationsManager manager : smList) {
			String folder = manager.getFolder();
			List<InteractiveExperiment> intExpList = manager.getExperimentsList();
			for (InteractiveExperiment anIntExpList : intExpList) {
				if (anIntExpList != null) {
					if (anIntExpList.getState ().equals (States.COMPLETED)) {
						files.put ("results" + File.separatorChar + folder + File.separatorChar + manager.getId () + File.separatorChar + anIntExpList.getInstanceName () + ".json", anIntExpList.getFinalSolution ());
					}
				}
			}

			List<Map<String, String>> inputFiles;
			try {
				inputFiles = fileService.getFiles (manager.getInputFolders(), ".txt");
			} catch (IOException e) {
				logger.error("Cannot download input files. Missing TXT file.", e);
				return;
			}
			files.put("input"+File.separatorChar+folder+File.separatorChar+manager.getInputFileName(),manager.getInput() );

			for (Map<String, String> inputFile : inputFiles) {
				files.put ("input" + File.separatorChar + folder + File.separatorChar + inputFile.get ("name"),
						inputFile.get ("content"));
			}
		}
		respondWithZipFile(files, response);
	}

	@RequestMapping(value="/downloadJson", method=RequestMethod.GET)
	@ResponseBody void downloadWIJson(@RequestParam(value="id") Long id, HttpServletResponse response)
			throws JsonProcessingException, IOException {
		SimulationsManager manager = simulationsManagerRepository.findOne(id);
		response.setContentType("application/json;charset=utf-8");
		response.setHeader( "Content-Disposition", "attachment;filename = " + manager.getInstanceName() + ".json" );
		response.getWriter().write(Compressor.decompress(manager.getInput()));
		response.getWriter().flush();
		response.getWriter().close();
	}

	@RequestMapping(value="/downloadFinalJson", method=RequestMethod.GET)
	@ResponseBody void downloadFinalSolOptJson(@RequestParam(value="id") Long id, HttpServletResponse response)
			throws JsonProcessingException, IOException {
		InteractiveExperiment exp = intExperimentRepository.findOne(id);
		response.setContentType("application/json;charset=utf-8");
		response.setHeader( "Content-Disposition", "attachment;filename = " + exp.getInstanceName()+ "SOL.json" );
		response.getWriter().write(Compressor.decompress(exp.getFinalSolution()));
		response.getWriter().flush();
		response.getWriter().close();
	}


	@RequestMapping(value="/downloadZipOptSim", method=RequestMethod.GET)
	@ResponseBody void downloadZipOptSimManInputs(@RequestParam(value="id") Long id,
												  HttpServletResponse response) {
		SimulationsManager manager = simulationsManagerRepository.findOne(id);
		List<Map<String, String>> inputFiles;
		try {
			inputFiles = fileService.getFiles (manager.getInputFolders(), ".txt");
		} catch (IOException e) {
			logger.error("Cannot download input files. Missing TXT file.", e);
			return;
		}

		Map<String, String> files = new HashMap<>();
		files.put(manager.getInputFileName(), manager.getInput() );

		for (Map<String, String> inputFile : inputFiles) {
			files.put (inputFile.get ("name"), inputFile.get ("content"));
		}
		respondWithZipFile(files, response);
	}

	@RequestMapping(value="/downloadZipOptInputFolder", method=RequestMethod.GET)
	@ResponseBody void downloadZipOptInputFolder(@RequestParam(value="folder") String folder,
												 HttpServletResponse response) {
		List<SimulationsManager> folderManagerList =  simulationsManagerRepository.findByFolderOrderByIdAsc(folder);
		Map<String, String> files = new HashMap<>();

		for (SimulationsManager manager : folderManagerList) {
			List<Map<String, String>> inputFiles;
			try {
				inputFiles = fileService.getFiles (manager.getInputFolders(), ".txt");
			} catch (IOException e) {
				logger.error("Cannot download input files. Missing TXT file.", e);
				return;
			}
			files.put(manager.getInputFileName(),manager.getInput() );

			for (Map<String, String> inputFile : inputFiles) {
				files.put (inputFile.get ("name"), inputFile.get ("content"));
			}
		}
		respondWithZipFile(files, response);
	}

	@RequestMapping(value="/downloadZipOptOutputJsons", method=RequestMethod.GET)
	@ResponseBody void downloadZipOptOutputJsons(@RequestParam(value="folder") String folder,
												 HttpServletResponse response) {
		List<SimulationsManager> folderManagerList = simulationsManagerRepository.findByFolderOrderByIdAsc(folder);
		Map<String, String> files = new HashMap<>();

		for (SimulationsManager manager : folderManagerList) {
			List<InteractiveExperiment> intExpList = manager.getExperimentsList ();
			for (InteractiveExperiment anIntExpList : intExpList) {
				if (anIntExpList != null) {
					if (anIntExpList.getState ().equals (States.COMPLETED)) {
						files.put (anIntExpList.getInstanceName () + ".json", anIntExpList.getFinalSolution ());
					}
				}
			}
		}
		respondWithZipFile(files, response);
	}

	private void respondWithZipFile(@NotNull Map<String, String> files, @NotNull HttpServletResponse response) {
		File zipPath = null;
		try {
			zipPath = fileUtility.createTempZip(files);
		} catch (IOException e) {
			logger.error("Problem creating zip file", e);
		}
		if (zipPath != null) {
			response.setContentType("application/zip");
			response.addHeader("Content-Disposition", "attachment; filename=\"test.zip\"");
			try {
				InputStream is = new FileInputStream(zipPath);
				IOUtils.copy(is, response.getOutputStream());
				response.flushBuffer();
			} catch (FileNotFoundException e) {
				logger.error("Zip file not found", e);
			} catch (IOException e) {
				logger.error("Cannot return zip file as an HTTP response", e);
			}
		}
	}
}
