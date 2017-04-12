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

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.launcher.controller.rest.BaseResponseBody;
import it.polimi.diceH2020.launcher.controller.rest.RestFilesController;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/files/view")
public class FilesController {

    private final Logger logger = Logger.getLogger (getClass ());

    @Setter(onMethod = @__(@Autowired))
    private RestFilesController internalController;

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String multipleSave(@RequestParam("file[]") List<MultipartFile> files,
                               @RequestParam("scenario") String useCase,
                               Model model, RedirectAttributes attributes) {
        ResponseEntity<BaseResponseBody> responseEntity = internalController.multipleSave (files, useCase);
        BaseResponseBody body = responseEntity.getBody ();

        Scenarios scenario = body.getScenario ();

        attributes.addAttribute("scenario", scenario);
        model.addAttribute("scenario", scenario);

        String returnedView;

        switch (responseEntity.getStatusCode ()) {
            case INTERNAL_SERVER_ERROR: {
                String message = body.getMessage ();
                if (message == null) message = "Unspecified server error";
                logger.error (message);
                model.addAttribute ("message", message);
                returnedView = "error";
                break;
            }
            case BAD_REQUEST: {
                model.addAttribute ("message", body.getMessage ());
                returnedView = "fileUpload";
                break;
            }
            case OK: {
                Long id = body.getSubmissionId ();
                attributes.addAttribute ("submissionId", id);
                returnedView = "redirect:/launch/view/submit";
                break;
            }
            default: {
                String message = "Unrecognized HTTP status";
                logger.error (message);
                model.addAttribute ("message", message);
                returnedView = "error";
            }
        }

        return returnedView;
    }

}
