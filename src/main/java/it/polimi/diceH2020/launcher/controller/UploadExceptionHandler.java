package it.polimi.diceH2020.launcher.controller;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class UploadExceptionHandler {

	private final Logger logger = Logger.getLogger(getClass());

	@ExceptionHandler(MultipartException.class)
	public ModelAndView handleError(MultipartException exception) {
		logger.error("error loading multipart request", exception);
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("message", exception.getMessage());
		modelAndView.setViewName("error");
		return modelAndView;
	}
}
