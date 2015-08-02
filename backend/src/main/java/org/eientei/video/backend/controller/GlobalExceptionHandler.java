package org.eientei.video.backend.controller;

import org.eientei.video.backend.orm.error.VideostreamerException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String, List<String>>> handleValidationFailure(MethodArgumentNotValidException e) {
        Map<String, List<String>> errors = new HashMap<String, List<String>>();

        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            List<String> strings = errors.get(fieldError.getField());
            if (strings == null) {
                strings = new ArrayList<String>();
                errors.put(fieldError.getField(), strings);
            }
            strings.add(fieldError.getCode());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<Map<String,List<String>>>(errors, headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(VideostreamerException.class)
    public ResponseEntity<Map<String, List<String>>> handleVideostreamerFailure(VideostreamerException e) {
        Map<String, List<String>> errors = new HashMap<String, List<String>>();
        ArrayList<String> strings = new ArrayList<String>();
        strings.add(e.getClass().getSimpleName());
        errors.put("form", strings);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<Map<String,List<String>>>(errors, headers, HttpStatus.BAD_REQUEST);
    }

}
