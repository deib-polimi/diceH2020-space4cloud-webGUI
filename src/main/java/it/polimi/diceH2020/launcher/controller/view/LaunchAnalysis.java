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
package it.polimi.diceH2020.launcher.controller.view;

import it.polimi.diceH2020.launcher.controller.rest.BaseResponseBody;
import it.polimi.diceH2020.launcher.controller.rest.RestLaunchAnalysisController;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@SessionAttributes("sim_manager") // it will persist in each browser tab,
// resolved with
// http://stackoverflow.com/questions/368653/how-to-differ-sessions-in-browser-tabs/11783754#11783754
@Controller
@RequestMapping("/launch/view")
public class LaunchAnalysis {

	private final Logger logger = Logger.getLogger (getClass ());

	@Setter(onMethod = @__(@Autowired))
	private RestLaunchAnalysisController internalController;

	@ModelAttribute("sim_manager")
	public SimulationsManager createSim_manager() {
		return new SimulationsManager();
	}

	@RequestMapping(value = "/error", method = RequestMethod.GET)
	public String showErrorView () {
		return "error";
	}

	@RequestMapping(value = "/submit", method = RequestMethod.GET)
	public String completeSubmission (Model model, @ModelAttribute("submissionId") Long submissionId,
									  RedirectAttributes redirectAttributes) {
		ResponseEntity<BaseResponseBody> response = internalController.submitById (submissionId);
		BaseResponseBody body = response.getBody ();

		model.addAttribute ("scenario", body.getScenario ());
		redirectAttributes.addAttribute ("scenario", body.getScenario ());

		String returnedView;
		switch (response.getStatusCode ()) {
			case BAD_REQUEST:
			case INTERNAL_SERVER_ERROR: {
				String message = body.getMessage ();
				if (message == null) message = "Unspecified server error";
				logger.error (message);
				redirectAttributes.addAttribute ("message", message);
				returnedView = "redirect:/launchRetry";
				break;
			}
			case OK:
				returnedView = "redirect:/";
				break;
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
