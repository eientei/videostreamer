package org.eientei.videostreamer.backend.controllers;

import org.eientei.videostreamer.backend.pojo.site.ErrorInfo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-15
 * Time: 21:49
 */
@ControllerAdvice
public class ErrorControllerAdvice {
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Object errorHandler(RuntimeException error) {
        ErrorInfo builder = new ErrorInfo();
        builder.setCode("error." + error.getClass().getSimpleName());
        builder.setText(error.getMessage());
        return builder;
    }

}
