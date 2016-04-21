package it.polimi.diceH2020.launcher.controller;

import org.apache.log4j.Logger;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;

@ControllerAdvice
public class UploadExceptionHandler {

	private final Logger logger = Logger.getLogger(getClass());

	@ExceptionHandler(MultipartException.class)
	public String handleError(MultipartException exception, Model modelAndView) {
		logger.error("error in multipart upload", exception);
		modelAndView.addAttribute("message", exception);
		return "error";
	}
}
