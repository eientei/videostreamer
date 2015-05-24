package org.eientei.videostreamer.controller;

import org.eientei.videostreamer.orm.error.VideostreamerException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-06
 * Time: 09:08
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String,List<String>> handleValidationFailure(MethodArgumentNotValidException e) {
        Map<String, List<String>> errors = new HashMap<String, List<String>>();

        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            List<String> strings = errors.get(fieldError.getField());
            if (strings == null) {
                strings = new ArrayList<String>();
                errors.put(fieldError.getField(), strings);
            }
            strings.add(fieldError.getCode());
        }

        return errors;
    }

    @ExceptionHandler(VideostreamerException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String,List<String>> handleVideostreamerFailure(VideostreamerException e) {
        Map<String, List<String>> errors = new HashMap<String, List<String>>();
        ArrayList<String> strings = new ArrayList<String>();
        strings.add(e.getClass().getSimpleName());
        errors.put("form", strings);
        return errors;
    }

}
