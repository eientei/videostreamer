package org.eientei.video.backend.orm.error;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-06
 * Time: 13:01
 */
public class VideostreamerException extends Exception {
    public VideostreamerException() {
    }

    public VideostreamerException(String message) {
        super(message);
    }

    public VideostreamerException(Throwable cause) {
        super(cause);
    }
}
