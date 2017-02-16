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
package it.polimi.diceH2020.launcher.controller.rest;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.launcher.FileService;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import it.polimi.diceH2020.launcher.utility.Compressor;
import it.polimi.diceH2020.launcher.utility.FileUtility;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.*;

@RestController
@RequestMapping(value = "/download")
public class DownloadsController {

	private final Logger logger = Logger.getLogger(getClass ());

	@Autowired
	private SimulationsManagerRepository simulationsManagerRepository;

	@Autowired
	private FileUtility fileUtility;

	@Autowired
	private FileService fileService;

	/**
	 * Inputs/Folder/(1 JSON, n TXT)
	 * Results/Folder/SimManager(for each provider)/(1 JSON)
	 *
	 */
	@RequestMapping(value = "/all", method = RequestMethod.GET)
	public void downloadAll(String type, HttpServletResponse response) {
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
	@RequestMapping(value = "/selected", method = RequestMethod.GET)
	public void downloadSelected(@RequestParam(value="folder[]") String[] folders, HttpServletResponse response) {
		List<SimulationsManager> smList = new ArrayList<>();

		for(String folderID : folders){
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

			List<Map<String, String>> inputFiles = retrieveInputFiles (manager);

			files.put("input"+File.separatorChar+folder+File.separatorChar+manager.getInputFileName(),
					manager.getInput());

			for (Map<String, String> inputFile : inputFiles) {
				files.put ("input" + File.separatorChar + folder + File.separatorChar + inputFile.get ("name"),
						inputFile.get ("content"));
			}
		}

		respondWithZipFile(files, response);
	}

	private @NotNull List<Map<String, String>> retrieveInputFiles(@NotNull SimulationsManager manager) {
		List<Map<String, String>> inputFiles = new ArrayList<> ();
		try {
			inputFiles.addAll (fileService.getFiles (manager.getInputFolders (), ".txt"));
		} catch (IOException e) {
			logger.error("Cannot download input files.", e);
		}
		return inputFiles;
	}

	@RequestMapping(value = "/input/zip", method = RequestMethod.GET)
	public void downloadInputZip (String folder, HttpServletResponse response) {
		List<SimulationsManager> folderManagerList = simulationsManagerRepository.findByFolderOrderByIdAsc(folder);
		Map<String, String> files = new HashMap<>();

		for (SimulationsManager manager : folderManagerList) {
			List<Map<String, String>> inputFiles = retrieveInputFiles (manager);

			files.put(manager.getInputFileName(), manager.getInput());

			for (Map<String, String> inputFile : inputFiles) {
				files.put (inputFile.get ("name"), inputFile.get ("content"));
			}
		}

		respondWithZipFile(files, response);
	}

	@RequestMapping(value = "/solution/zip", method = RequestMethod.GET)
	public void downloadSolutionZip (String folder, HttpServletResponse response) {
		List<SimulationsManager> folderManagerList = simulationsManagerRepository.findByFolderOrderByIdAsc(folder);
		Map<String, String> files = new HashMap<>();

		for (SimulationsManager manager : folderManagerList) {
			List<InteractiveExperiment> experimentsList = manager.getExperimentsList ();
			for (InteractiveExperiment experiment : experimentsList) {
				if (experiment != null) {
					if (experiment.getState ().equals (States.COMPLETED)) {
						files.put (experiment.getInstanceName () + ".json", experiment.getFinalSolution ());
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
			try (BufferedInputStream is = new BufferedInputStream (new FileInputStream(zipPath))) {
				IOUtils.copy(is, response.getOutputStream());
				response.flushBuffer();
			} catch (FileNotFoundException e) {
				logger.error("Zip file not found", e);
			} catch (IOException e) {
				logger.error("Cannot return zip file as an HTTP response", e);
			}
		}
	}

	@RequestMapping(value = "/solution/json/{id}", method = RequestMethod.GET)
	public ResponseEntity<String> downloadSolutionJson(@PathVariable Long id) {
		ResponseEntity<String> output = new ResponseEntity<> (HttpStatus.NOT_FOUND);

		SimulationsManager manager = simulationsManagerRepository.findById (id);
		if (manager != null) {
			for (InteractiveExperiment experiment : manager.getExperimentsList ()) {
				// TODO there is always only one InteractiveExperiment, all the monster should be treated accordingly
				try {
					String solution = Compressor.decompress (experiment.getFinalSolution ());
					output = new ResponseEntity<> (solution, HttpStatus.OK);
				} catch (IOException e) {
					logger.error (String.format ("Could not decompress solutions JSON for experiment %d", id), e);
					output = new ResponseEntity<> (HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}
		}

		return output;
	}

	@RequestMapping(value = "/solution/json", method = RequestMethod.GET)
	public List<SimulationsManagerRepresentation> downloadSolutionJson() {
		List<SimulationsManagerRepresentation> managerRepresentations = new LinkedList<> ();

		List<SimulationsManager> managers = simulationsManagerRepository.findByState (States.COMPLETED);
		for (SimulationsManager manager : managers) {
			Link link = ControllerLinkBuilder.linkTo (
					ControllerLinkBuilder.methodOn (getClass ()).downloadSolutionJson (manager.getId ())
			).withRel ("solution");

			SimulationsManagerRepresentation representation = new SimulationsManagerRepresentation (manager);
			representation.add (link);
			managerRepresentations.add (representation);
		}

		return managerRepresentations;
	}
}
