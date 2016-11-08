package org.eientei.videostreamer.impl.amf;

import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class AmfListTest {

    @Test
    public void testCastingList() {
        AmfList list = new AmfList();
        list.add("abc");
        list.add(123.0);
        list.add(new Object());

        String str = list.getAs(0);
        Double num = list.getAs(1);
        Object obj = list.getAs(2);
    }
}
