package it.polimi.diceH2020.launcher.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import it.polimi.diceH2020.launcher.repository.ExperimentRepository;

@Controller
@RequestMapping("/complete")
public class ExperimentController {

	@Autowired
	ExperimentRepository expRepository;
	
	@RequestMapping(value="/experiments", method=RequestMethod.GET)
	public String listExperiments(Model model) {
			model.addAttribute("experiments", expRepository.findAll());
			return "experimentsList";
	}
}
