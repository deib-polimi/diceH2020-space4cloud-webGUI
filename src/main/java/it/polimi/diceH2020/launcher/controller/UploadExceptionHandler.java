package it.polimi.diceH2020.launcher.controller;

import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;


@ControllerAdvice
public class UploadExceptionHandler {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UploadExceptionHandler.class.getName());
	
//	@ExceptionHandler(MaxUploadSizeExceededException.class)
//    public ModelAndView handleMaxUploadSizeExceededException(){
//    	logger.info("Max file size exceeded");
//		ModelAndView modelAndView = new ModelAndView();
//		modelAndView.addObject("message", "Max file size exceeded");
//		modelAndView.setViewName("error");
//		return modelAndView;
//    }
	
	@ExceptionHandler(MultipartException.class)
	public String handleError(MultipartException exception,Model modelAndView) {
		logger.info("MultipartUploadException: "+ exception);
	    modelAndView.addAttribute("message", exception);
	    return "error";
	}
	
//	@ExceptionHandler(value = Exception.class)
//    public ModelAndView defaultErrorHandler(HttpServletRequest req, Exception exception) throws Exception {
//		ModelAndView modelAndView = new ModelAndView();
//	    modelAndView.addObject("message", exception.getMessage());
//	    modelAndView.setViewName("error");
//        return modelAndView;
//    }
}
