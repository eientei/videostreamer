package org.eientei.video.backend.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-09
 * Time: 07:41
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class HttpNotFoundException extends RuntimeException {
}
