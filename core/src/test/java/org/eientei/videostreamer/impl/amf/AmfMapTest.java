package org.eientei.videostreamer.impl.amf;

import org.junit.Test;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class AmfMapTest {

    @Test
    public void testCastingMap() {
        AmfMap map = new AmfMap();
        map.put("abc", "efg");
        map.put("ddd", 123.0);
        map.put("ccc", new Object());

        String str = map.getAs("abc");
        Double ddd = map.getAs("ddd");
        Object ccc = map.getAs("ccc");
    }
}
