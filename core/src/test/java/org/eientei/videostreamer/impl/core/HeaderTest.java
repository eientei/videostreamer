package org.eientei.videostreamer.impl.core;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class HeaderTest {
    @Test
    public void testHeader() {
        Header header = new Header(3, 0, Message.Type.ACK, 1);

        Assert.assertEquals(3, header.getChunk());
        Assert.assertEquals(0, header.getTime());
        Assert.assertEquals(-1, header.getLength());
        Assert.assertEquals(Message.Type.ACK, header.getType());
        Assert.assertEquals(1, header.getStream());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHeaderInvalidChunk() {
        new Header(0, 0, Message.Type.ACK, 1);
    }

    @Test
    public void testHeaderTypes() {
        Assert.assertEquals(0, Header.Type.FULL.getValue());
        Assert.assertEquals(1, Header.Type.MEDIUM.getValue());
        Assert.assertEquals(2, Header.Type.SMALL.getValue());
        Assert.assertEquals(3, Header.Type.NONE.getValue());
    }
}
