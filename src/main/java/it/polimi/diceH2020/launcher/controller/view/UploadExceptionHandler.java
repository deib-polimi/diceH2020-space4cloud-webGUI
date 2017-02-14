/*
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
