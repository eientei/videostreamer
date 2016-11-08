package org.eientei.videostreamer.impl.core;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class TrackTest {

    public class TrackTesting extends Track {
        public TrackTesting(int timescale, int frametick) {
            super(timescale, frametick);
        }

        @Override
        public Sample makeSample(Message message) throws Exception {
            return null;
        }

        @Override
        protected void deallocate() {

        }
    }

    @Test
    public void testTrack() {
        Track track = new TrackTesting(1000,24);
        Assert.assertEquals(1000, track.getTimescale());
        Assert.assertEquals(24, track.getFrametick());

        Assert.assertSame(track, track.touch());
    }
}