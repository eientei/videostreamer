package org.eientei.videostreamer.controller.validation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Map;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-22
 * Time: 15:28
 */
public class ReCaptchaValidator implements ConstraintValidator<ReCaptcha, String> {
    @Autowired
    private HttpServletRequest request;

    @Value("${videostreamer.captcha:false}")
    private boolean captcha;

    @Value("${videostreamer.captcha.secret:}")
    private String secret;

    private RestTemplate restTemplate;

    @Override
    public void initialize(ReCaptcha constraintAnnotation) {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        restTemplate = new RestTemplate();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!captcha) {
            return true;
        }
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        MultiValueMap<String, String> data = new LinkedMultiValueMap<String, String>();
        data.add("secret", secret);
        data.add("response", value);
        data.add("remoteip", request.getRemoteAddr());
        ResponseEntity<Map> response = restTemplate.postForEntity("https://www.google.com/recaptcha/api/siteverify", data, Map.class);

        return (Boolean) response.getBody().get("success");
    }
}
