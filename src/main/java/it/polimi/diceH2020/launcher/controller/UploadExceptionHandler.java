package it.polimi.diceH2020.launcher.controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class UploadExceptionHandler {
	@ExceptionHandler(MultipartException.class)
	  public ModelAndView handleError(MultipartException exception) {
	    ModelAndView modelAndView = new ModelAndView();
	    modelAndView.addObject("message", exception.getMessage());
	    modelAndView.setViewName("error");
	    return modelAndView;
	  }
}
