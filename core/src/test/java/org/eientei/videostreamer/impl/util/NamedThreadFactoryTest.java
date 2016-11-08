package org.eientei.videostreamer.impl.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-03
 */
public class NamedThreadFactoryTest {
    @Test
    public void testThreadFactory() {
        NamedThreadFactory test = new NamedThreadFactory("test");
        Thread thread = test.newThread(new Runnable() {
            @Override
            public void run() {
            }
        });

        Assert.assertEquals("test-1", thread.getName());
    }
}