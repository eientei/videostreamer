package org.eientei.videostreamer.backend.errors;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-17
 * Time: 11:48
 */
public class CaptchaVerificationError extends RuntimeException {
    public CaptchaVerificationError() {
        super("Incorrect CAPTCHA");
    }
}
