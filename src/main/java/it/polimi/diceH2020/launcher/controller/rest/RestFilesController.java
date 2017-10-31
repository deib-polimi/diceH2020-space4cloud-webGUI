/*
Copyright 2017 Eugenio Gianniti
Copyright 2016 Jacopo Rigoli
Copyright 2016 Michele Ciavotta

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
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenario;
import it.polimi.diceH2020.launcher.model.PendingSubmission;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.service.Validator;
import it.polimi.diceH2020.launcher.utility.FileNameClashException;
import it.polimi.diceH2020.launcher.utility.FileUtility;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/files")
public class RestFilesController {
    private final Logger logger = Logger.getLogger (getClass ());

    @Setter(onMethod = @__(@Autowired))
    private DiceService diceService;

    @Setter(onMethod = @__(@Autowired))
    private FileUtility fileUtility;

    @Setter(onMethod = @__(@Autowired))
    private Validator validator;

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public ResponseEntity<BaseResponseBody> multipleSave(@RequestParam("file[]") List<MultipartFile> files,
                                                         @RequestParam("scenario") String scenarioStringRepresentation) {
        BaseResponseBody body = new BaseResponseBody ();
        PendingSubmission submission = new PendingSubmission ();

        Scenario scenario = Scenario.generateScenario(scenarioStringRepresentation);
        body.setScenario (scenario);
        submission.setScenario (scenario);

        body.setAcceptedFiles (new LinkedList<> ());
        ResponseEntity<BaseResponseBody> response = new ResponseEntity<> (body, HttpStatus.INTERNAL_SERVER_ERROR);

        boolean good_status = true;
        List<String> additionalFileNames = new LinkedList<>();
        List<File> allSavedFiles = new LinkedList<> ();

        if (files == null || files.isEmpty ()) {
            String message = "No files to process!";
            logger.error (message);
            body.setMessage (message);
            response = new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
            good_status = false;
        } else {
            Iterator<MultipartFile> multipartFileIterator = files.iterator ();

            while (good_status && multipartFileIterator.hasNext ()) {
                MultipartFile multipartFile = multipartFileIterator.next ();
                String fileName = new File (multipartFile.getOriginalFilename ()).getName ();
                logger.trace("Analyzing file " + fileName);
                File savedFile = null;
                try {
                    savedFile = saveFile (multipartFile, fileName);
                    allSavedFiles.add (savedFile);
                } catch (FileNameClashException e) {
                    String message = String.format ("'%s' already exists", fileName);
                    logger.error (message, e);
                    body.setMessage (message);
                    response = new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
                    good_status = false;
                } catch (IOException e) {
                    String message = String.format ("Error handling '%s'", fileName);
                    logger.error (message, e);
                    body.setMessage (message);
                    response = new ResponseEntity<> (body, HttpStatus.INTERNAL_SERVER_ERROR);
                    good_status = false;
                }

                if (good_status) {
                    if (fileName.contains (".json")) {
                        Optional<InstanceDataMultiProvider> idmp =
                                validator.readInstanceDataMultiProvider (savedFile.toPath ());
                        if (idmp.isPresent ()) {
                            if (idmp.get ().validate ()) {
                                submission.setInstanceData (savedFile.getPath ());
                                body.getAcceptedFiles ().add (savedFile.getName ());
                            } else {
                                logger.error (idmp.get ().getValidationError ());
                                body.setMessage (idmp.get ().getValidationError ());
                                response = new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
                                good_status = false;
                            }
                        } else {
                            String message = "You have submitted an invalid json!";
                            logger.error (message);
                            body.setMessage (message);
                            response = new ResponseEntity<> (body, HttpStatus.BAD_REQUEST);
                            good_status = false;
                        }
                    } else if (fileName.contains (".txt") || fileName.contains (".jsimg")
                            || fileName.contains (".def") || fileName.contains (".net")
                            || fileName.contains (".stat")) {
                        additionalFileNames.add (savedFile.getPath ());
                        body.getAcceptedFiles ().add (savedFile.getName ());
                    }
                }
            }
        }

        if (good_status) {
            body.setMessage ("Successful file upload");

            submission.setPaths (additionalFileNames);
            diceService.updateSubmission (submission);

            body.setSubmissionId (submission.getId ());
            Link submissionLink = ControllerLinkBuilder.linkTo (
                    ControllerLinkBuilder.methodOn (RestLaunchAnalysisController.class)
                            .submitById (submission.getId ())
            ).withRel ("submit");
            body.add (submissionLink);

            logger.info (body);
            response = new ResponseEntity<> (body, HttpStatus.OK);
        } else {
            if (fileUtility.delete (allSavedFiles)) {
                logger.debug ("Deleted the files created during a failed submission");
            }
        }

        return response;
    }

    private File saveFile(MultipartFile file, String fileName) throws FileNameClashException, IOException {
        File outFile = fileUtility.provideFile(fileName);
        try (BufferedOutputStream buffStream = new BufferedOutputStream(new FileOutputStream (outFile))) {
            byte[] bytes = file.getBytes();
            buffStream.write(bytes);
        }
        return outFile;
    }
}
