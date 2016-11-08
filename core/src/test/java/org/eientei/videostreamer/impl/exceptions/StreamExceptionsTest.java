package org.eientei.videostreamer.impl.exceptions;

import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class StreamExceptionsTest {
    @Test(expected = StreamAlreadyPublishingException.class)
    public void testStreamAlreadyPublishing() throws StreamAlreadyPublishingException {
        throw new StreamAlreadyPublishingException();
    }

    @Test(expected = StreamAlreadySubscribedException.class)
    public void testStreamAlreadySubscribed() throws StreamAlreadySubscribedException {
        throw new StreamAlreadySubscribedException();
    }

    @Test(expected = StreamNotOwnedException.class)
    public void testStreamNotOwned() throws StreamNotOwnedException {
        throw new StreamNotOwnedException();
    }

    @Test(expected = StreamNotPublishedException.class)
    public void testStreamNotPublished() throws StreamNotPublishedException {
        throw new StreamNotPublishedException();
    }

    @Test(expected = StreamNotSubscribedException.class)
    public void testStreamNotSubscribed() throws StreamNotSubscribedException {
        throw new StreamNotSubscribedException();
    }
}
