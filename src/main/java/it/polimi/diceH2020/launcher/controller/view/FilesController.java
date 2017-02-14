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
package it.polimi.diceH2020.launcher.controller.view;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.launcher.service.Validator;
import it.polimi.diceH2020.launcher.utility.FileNameClashException;
import it.polimi.diceH2020.launcher.utility.FileUtility;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

@Controller
@RequestMapping("/files")
public class FilesController {

    private final Logger logger = Logger.getLogger (getClass ());

    @Autowired
    private FileUtility fileUtility;

    @Autowired
    private Validator validator;

    @RequestMapping(value = "/upload", method = RequestMethod.GET)
    public @ResponseBody String hello() {
        return "done";
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String multipleSave(@RequestParam("file[]") MultipartFile[] files, @RequestParam("scenario") String useCase,
                               Model model, RedirectAttributes redirectAttrs) {
        Scenarios scenario = Scenarios.valueOf(useCase);
        ArrayList<String> tmpValues = new ArrayList<>();

        redirectAttrs.addAttribute("scenario", scenario);
        model.addAttribute("scenario", scenario);

        if (files == null || files.length == 0) {
            String message = "Wrong files!";
            logger.error (message);
            model.addAttribute("message", message);
            return "launchSimulation_FileUpload";
        }

        for (MultipartFile multipartFile: files) {
            String fileName = new File (multipartFile.getOriginalFilename ()).getName ();

            File savedFile;
            try {
                savedFile = saveFile (multipartFile, fileName);
            } catch (FileNameClashException e) {
                String message = String.format ("'%s' already exists", fileName);
                logger.error (message, e);
                model.addAttribute ("message", message);
                return "launchSimulation_FileUpload";
            } catch (IOException e) {
                String message = String.format ("Error handling '%s'", fileName);
                logger.error (message, e);
                model.addAttribute ("message", message);
                return "error";
            }

            if (fileName.contains (".json")) {
                Optional<InstanceDataMultiProvider> idmp =
                        validator.readInstanceDataMultiProvider (savedFile.toPath ());
                if (idmp.isPresent ()) {
                    if (idmp.get ().validate ()) {
                        redirectAttrs.addAttribute ("instanceDataMultiProvider", savedFile.getPath ());
                    } else {
                        logger.error (idmp.get ().getValidationError ());
                        model.addAttribute ("message", idmp.get ().getValidationError ());
                        return "launchSimulation_FileUpload";
                    }
                } else {
                    String message = "You have submitted an invalid json!";
                    logger.error (message);
                    model.addAttribute ("message", message);
                    return "launchSimulation_FileUpload";
                }
            } else if (fileName.contains (".txt") || fileName.contains (".def") || fileName.contains (".net")) {
                tmpValues.add (savedFile.getPath ());
            }
        }

        redirectAttrs.addAttribute("pathList", tmpValues);
        return "redirect:/launch/simulationSetup";
    }

    private File saveFile(MultipartFile file, String fileName) throws FileNameClashException, IOException {
        File outFile = fileUtility.provideFile(fileName);
        try (BufferedOutputStream buffStream = new BufferedOutputStream(new FileOutputStream(outFile))) {
            byte[] bytes = file.getBytes();
            buffStream.write(bytes);
        }
        return outFile;
    }

}
