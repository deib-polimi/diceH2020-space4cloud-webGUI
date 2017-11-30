/*
Copyright 2017 Eugenio Gianniti
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

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobProfile;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenario;
import it.polimi.diceH2020.launcher.model.PendingSubmission;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.PendingSubmissionRepository;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.service.Validator;
import it.polimi.diceH2020.launcher.utility.Compressor;
import it.polimi.diceH2020.launcher.utility.FileNameClashException;
import it.polimi.diceH2020.launcher.utility.FileUtility;
import it.polimi.diceH2020.launcher.utility.JsonSplitter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/launch")
public class RestLaunchAnalysisController {
   private final Logger logger = Logger.getLogger (getClass ());

   @Setter(onMethod = @__(@Autowired))
   private Validator validator;

   @Setter(onMethod = @__(@Autowired))
   private DiceService diceService;

   @Setter(onMethod = @__(@Autowired))
   private FileUtility fileUtility;

   @Setter(onMethod = @__(@Autowired))
   private PendingSubmissionRepository submissionRepository;

   @RequestMapping(value = "/submit", method = RequestMethod.GET)
   public List<PendingSubmissionRepresentation> getPendingSubmissions () {
      List<PendingSubmission> submissions = submissionRepository.findAll ();
      return submissions.stream ().map (this::prepareRepresentation).collect (Collectors.toList ());
   }

   @RequestMapping(value = "/submit", method = RequestMethod.DELETE)
   public void deletePendingSubmissions () {
      for (PendingSubmission submission: submissionRepository.findAll ()) {
         cleanup (submission.getId (), null);
         logger.info ("All the pending submissions have been deleted.");
      }
   }

   @RequestMapping(value = "/submit/{id}", method = RequestMethod.GET)
   public ResponseEntity<PendingSubmissionRepresentation> getPendingSubmission (@PathVariable Long id) {
      ResponseEntity<PendingSubmissionRepresentation> response;

      PendingSubmission submission = submissionRepository.findOne (id);
      if (submission == null) {
         response = new ResponseEntity<> (HttpStatus.NOT_FOUND);
      } else {
         PendingSubmissionRepresentation representation = prepareRepresentation (submission);
         response = new ResponseEntity<> (representation, HttpStatus.OK);
      }

      return response;
   }

   private PendingSubmissionRepresentation prepareRepresentation (PendingSubmission submission) {
      Link link = ControllerLinkBuilder.linkTo (
            ControllerLinkBuilder.methodOn (getClass ()).getPendingSubmission (submission.getId ())
            ).withSelfRel ();

      PendingSubmissionRepresentation representation = new PendingSubmissionRepresentation (submission);
      representation.add (link);

      return representation;
   }

   @RequestMapping(value = "/submit/{id}", method = RequestMethod.DELETE)
   public ResponseEntity<?> deletePendingSubmission (@PathVariable Long id) {
      ResponseEntity<?> response = new ResponseEntity<> (HttpStatus.NOT_FOUND);

      if (submissionRepository.exists (id)) {
         cleanup (id, null);
         logger.info (String.format ("Pending submission %d has been deleted.", id));
         response = new ResponseEntity<> (HttpStatus.OK);
      }

      return response;
   }

   @RequestMapping(value = "/submit/{id}", method = RequestMethod.POST)
   public ResponseEntity<BaseResponseBody> submitById (@PathVariable Long id) {
      BaseResponseBody body = new BaseResponseBody ();

      PendingSubmission submission = submissionRepository.findOne (id);

      if (submission == null) {
         body.setMessage (String.format ("A submission with ID %d is not pending", id));
         return new ResponseEntity<> (body, HttpStatus.NOT_FOUND);
      }

      Scenario scenario = submission.getScenario ();
      body.setScenario (scenario);

      List<String> pathList = submission.getPaths ();

      if (pathList == null || pathList.isEmpty ()) {
         cleanup (id, null);
         String message = "You haven't submitted any file!";
         logger.error (message);
         body.setMessage (message);
         return new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
      }

      String instanceDataMultiProviderPath = submission.getInstanceData ();
      if (instanceDataMultiProviderPath == null) {
         cleanup(id, null);
         String message = "Select a Json file!";
         logger.error (message);
         body.setMessage (message);
         return new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
      }

      Optional<InstanceDataMultiProvider> maybeInstanceData =
         validator.readInstanceDataMultiProvider(Paths.get(instanceDataMultiProviderPath));

      if (maybeInstanceData.isPresent()) {
         if (! maybeInstanceData.get().validate()) {
            cleanup(id, null);
            String message = maybeInstanceData.get().getValidationError();
            logger.error (message);
            body.setMessage (message);
            return new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
         }
      } else {
         cleanup(id, null);
         String message = "Error with InstanceDataMultiProvider";
         logger.error (message);
         body.setMessage (message);
         return new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
      }

      InstanceDataMultiProvider instanceDataMultiProvider = maybeInstanceData.get();
      instanceDataMultiProvider.setScenario(scenario);

      String check = scenarioValidation(instanceDataMultiProvider);
      if (! check.equals("ok")) {
         cleanup(id, null);
         logger.error (check);
         body.setMessage (check);
         return new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
      }

      List<InstanceDataMultiProvider> inputList =
         JsonSplitter.splitInstanceDataMultiProvider(instanceDataMultiProvider);

      if (inputList.size() > 1) {
         List<String> providersList = inputList.stream().map(InstanceDataMultiProvider::getProvider)
            .filter (Objects::nonNull).collect(Collectors.toList());
         if (! minNumTxt(providersList, pathList)) {
            cleanup(id, null);
            String message = "Not enough TXT files selected.\nFor each provider in your JSON there must be 2 TXT files containing in their name the provider name.";
            logger.error (message);
            body.setMessage (message);
            return new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
         }
      }

      List<SimulationsManager> simManagerList = initializeSimManagers(inputList);
      List<File> generatedFiles = new ArrayList<>();

      for (SimulationsManager sm: simManagerList) {
         sm.setInputFileName(Paths.get(instanceDataMultiProviderPath).getFileName().toString());
         InstanceDataMultiProvider input = sm.getInputData();

         File txtFolder;
         try {
            txtFolder = fileUtility.createInputSubFolder();
            generatedFiles.add(txtFolder);
         } catch (FileNameClashException e) {
            cleanup(id, generatedFiles);
            String message = "Too many folders for TXTs with the same name have been created!";
            logger.error (message, e);
            body.setMessage (message);
            return new ResponseEntity<> (body, HttpStatus.INTERNAL_SERVER_ERROR);
         } catch (IOException e) {
            cleanup (id, generatedFiles);
            String message = "Could not create a folders for TXTs!";
            logger.error (message, e);
            body.setMessage (message);
            return new ResponseEntity<> (body, HttpStatus.INTERNAL_SERVER_ERROR);
         }

         for (Map.Entry<String, Map<String, Map<String, JobProfile>>> jobIDs :
               input.getMapJobProfiles().getMapJobProfile().entrySet()) {
            for (Map.Entry<String, Map<String, JobProfile>> provider : jobIDs.getValue().entrySet()) {
               for (Map.Entry<String, JobProfile> typeVMs : provider.getValue().entrySet()) {

                  String secondPartOfTXTName =
                     getSecondPartOfReplayerFileName (jobIDs.getKey(), provider.getKey(), typeVMs.getKey());

                  List<String> txtToBeSaved = pathList.stream().filter(s -> s.contains(secondPartOfTXTName))
                     .filter(s->s.contains(input.getId())).collect(Collectors.toList());

                  if (txtToBeSaved.isEmpty()) {
                     cleanup(id, generatedFiles);
                     String message = String.format ("Missing TXT file for Instance: %s, Job: %s, Provider: %s, TypeVM: %s",
                           input.getId(), jobIDs.getKey(), provider.getKey(), typeVMs.getKey());
                     logger.error (message);
                     body.setMessage (message);
                     return new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
                  }

                  for (String srcPath: txtToBeSaved) {
                     File src = new File(srcPath);

                     String fileContent;
                     try (FileReader reader = new FileReader (src)) {
                        fileContent = IOUtils.toString (reader);
                        String compressedContent = Compressor.compress(fileContent);

                        File generatedFile = fileUtility.createInputFile(txtFolder, src.getName());
                        fileUtility.writeContentToFile(compressedContent, generatedFile);

                        generatedFiles.add(generatedFile);
                     } catch (FileNameClashException e) {
                        cleanup(id, generatedFiles);
                        body.setMessage (e.getMessage());
                        logger.error("Cannot create file due to filename clash", e);
                        return new ResponseEntity<> (body, HttpStatus.INTERNAL_SERVER_ERROR);
                     } catch (IOException e) {
                        cleanup(id, generatedFiles);
                        String message = String.format (
                              "Problem with TXT paths. [TXT file for Instance: %s, Job: %s, Provider: %s, TypeVM: %s]",
                              input.getId(), jobIDs.getKey(), provider.getKey(), typeVMs.getKey());
                        logger.error (message, e);
                        body.setMessage (message);
                        return new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
                     }

                     if (fileContent.isEmpty ()) {
                        cleanup(id, generatedFiles);
                        String message = String.format (
                              "Missing TXT file content for Instance: %s, Job: %s, Provider: %s, TypeVM: %s",
                              input.getId(), jobIDs.getKey(), provider.getKey(), typeVMs.getKey());
                        logger.error (message);
                        body.setMessage (message);
                        return new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
                     }
                  }
               }
            }
               }

         sm.addInputFolder(txtFolder.getPath ());
         sm.setNumCompletedSimulations(0);
         sm.buildExperiments();
      }

      cleanup(id, null);

      for (SimulationsManager simulationsManager : simManagerList) {
         logger.trace("Simulation with " + simulationsManager.toString() + " - Scenario is " + simulationsManager.getScenario().getStringRepresentation());
         diceService.simulation(simulationsManager);

         Link link = ControllerLinkBuilder.linkTo (
               ControllerLinkBuilder.methodOn (DownloadsController.class)
               .downloadSolutionJson (simulationsManager.getId ())
               ).withRel ("solution");
         body.add (link);
      }

      return new ResponseEntity<> (body, HttpStatus.OK);
   }

   private String getSecondPartOfReplayerFileName (String idC, String provider, String vmType) {
      return String.format ("J%s%s%s", idC, provider, vmType);
   }

   private String scenarioValidation(InstanceDataMultiProvider instanceDataMultiProvider) {
      String returnString = "ok";
      Scenario scenario = instanceDataMultiProvider.getScenario();

      if (instanceDataMultiProvider.getMapJobProfiles() == null ||
            instanceDataMultiProvider.getMapClassParameters() == null) {
         returnString = "Json is missing some required parameters(MapJobProfiles or MapClassParameters)!";
      } else {
         switch (scenario.getCloudType()) {
            case PRIVATE:
               if(scenario.getAdmissionControl()) {
                  if (instanceDataMultiProvider.getPrivateCloudParameters() == null
                        || instanceDataMultiProvider.getMapVMConfigurations() == null) {
                     returnString = "Json is missing some required parameters(PrivateCloudParameters or MapVMConfigurations)!";
                  } else if (! instanceDataMultiProvider.getPrivateCloudParameters().validate()) {
                     returnString = "Private Cloud Parameters uploaded in Json aren't valid!";
                  } else if (! instanceDataMultiProvider.getMapVMConfigurations().validate()) {
                     returnString = "VM Configurations uploaded in Json aren't valid!";
                  } else if (instanceDataMultiProvider.getProvidersList().size() != 1) {
                     returnString = "A private scenario cannot have multiple providers!(call you providers:\"inHouse\")";
                  }
               } else {
                  if (instanceDataMultiProvider.getMapVMConfigurations() == null) {
                     returnString = "Json is missing some required parameters(MapVMConfigurations)!";
                  } else if (instanceDataMultiProvider.getMapVMConfigurations().getMapVMConfigurations() == null) {
                     returnString = "Json is missing some required parameters(MapVMConfigurations)!";
                  } else if (!instanceDataMultiProvider.getMapVMConfigurations().validate()) {
                     returnString = "VM Configurations uploaded in Json aren't valid!";
                  } else if (instanceDataMultiProvider.getProvidersList().size() != 1) {
                     returnString = "A private scenario cannot have multiple providers!(call you providers:\"inHouse\")";
                  }
               }
               break;
            case PUBLIC: 
               if(scenario.getUseComplexPricingModel()) {
                  if (instanceDataMultiProvider.getMapPublicCloudParameters() == null ||
                        instanceDataMultiProvider.getMapPublicCloudParameters().getMapPublicCloudParameters() == null) {
                     returnString = "Json is missing some required parameters(MapPublicCloudParameters)!";
                  } else if (! instanceDataMultiProvider.getMapPublicCloudParameters().validate()) {
                     returnString = "Public Cloud Parameters uploaded in Json aren't valid!";
                  }
               }
               break;
            default:
               String errorMessage = String.format ("Scenario not implemented: '%s'", scenario.toString ());
               logger.error (errorMessage);
               throw new RuntimeException (errorMessage);
         }
      }
      return returnString;
   }

   private List<SimulationsManager> initializeSimManagers(List<InstanceDataMultiProvider> inputList) {
      List<SimulationsManager> simManagerList = new ArrayList<>();
      String folder = fileUtility.generateUniqueString();

      for (InstanceDataMultiProvider instanceData : inputList) {
         SimulationsManager simManager = new SimulationsManager ();
         simManager.setInputData (instanceData);
         simManager.setScenario(instanceData.getScenario());
         simManager.setFolder (folder);

         simManagerList.add (simManager);
      }

      return simManagerList;
   }

   private void cleanup (Long id, List<File> generated) {
      PendingSubmission submission = submissionRepository.findOne (id);

      String jsonPath = submission.getInstanceData ();
      if (jsonPath != null) {
         File inputJSONFile = new File (jsonPath);
         fileUtility.delete (inputJSONFile);
      }

      List<String> inputPaths = submission.getPaths ();
      deleteUploadedFiles(inputPaths);

      if (generated != null) fileUtility.delete(generated);

      submissionRepository.delete (id);
   }

   private void deleteUploadedFiles(List<String> pathList){
      List<File> filesToErase = new ArrayList<>();
      for(String path: pathList){
         filesToErase.add(new File(path));
      }
      fileUtility.delete(filesToErase);
   }

   /**
    * With multi provider different TXTs should be provided
    */
   private boolean minNumTxt(List<String> providers, List<String> pathList){
      for (String provider: providers) {
         if (pathList.stream ().filter (s -> s.contains(provider)).count () < 2) return false;
      }
      return true;
   }
}
