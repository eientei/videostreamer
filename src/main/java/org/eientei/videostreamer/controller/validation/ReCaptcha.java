package org.eientei.videostreamer.controller.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-22
 * Time: 15:27
 */
@Constraint(validatedBy = ReCaptchaValidator.class)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RUNTIME)
public @interface ReCaptcha {
    String message() default "Invalid ReCaptcha";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
