package org.eientei.videostreamer.impl.amf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class AmfTest {

    @Test
    public void testVerbatim() {
        ByteBuf buf = Unpooled.buffer();
        Amf.serialize(buf, 1.0);
        Assert.assertEquals(1.0, Amf.deserialize(buf));

        Amf.serialize(buf, true);
        Assert.assertEquals(true, Amf.deserialize(buf));

        Amf.serialize(buf, "abc");
        Assert.assertEquals("abc", Amf.deserialize(buf));

        List<Object> list = new ArrayList<>();
        list.add("aa");
        list.add(1.0);
        list.add(true);
        list.add(null);

        Amf.serialize(buf, list);
        List<Object> reslist = Amf.deserialize(buf);
        Assert.assertEquals(list, reslist);
        Assert.assertEquals("aa", reslist.get(0));
        Assert.assertEquals(1.0, reslist.get(1));
        Assert.assertEquals(true, reslist.get(2));
        Assert.assertEquals(null, reslist.get(3));


        Map<String, Object> map = new HashMap<>();
        map.put("str", "efg");
        map.put("num", 1.0);
        map.put("bln", true);
        map.put("nil", null);
        map.put("arr", list);

        Amf.serialize(buf, map);
        Map<String, Object> resmap = Amf.deserialize(buf);
        Assert.assertEquals("efg", resmap.get("str"));
        Assert.assertEquals(1.0, resmap.get("num"));
        Assert.assertEquals(true, resmap.get("bln"));
        Assert.assertEquals(null, resmap.get("nil"));
        Assert.assertEquals(list, resmap.get("arr"));

        Amf.serialize(buf, new AmfMap(map));
        AmfMap cstmap = Amf.deserialize(buf);
        Assert.assertEquals("efg", cstmap.get("str"));
        Assert.assertEquals(1.0, cstmap.get("num"));
        Assert.assertEquals(true, cstmap.get("bln"));
        Assert.assertEquals(null, cstmap.get("nil"));
        Assert.assertEquals(list, cstmap.get("arr"));

        buf.release();
    }

    @Test
    public void testVerbatimAll() {
        ByteBuf buf = Unpooled.buffer();

        Amf.serialize(buf, 1.0, "abc", true, null);
        AmfList list = Amf.deserializeAll(buf);
        Assert.assertEquals(1.0, list.get(0));
        Assert.assertEquals("abc", list.get(1));
        Assert.assertEquals(true, list.get(2));
        Assert.assertEquals(null, list.get(3));

        buf.release();
    }

    @Test
    public void testNew() {
        Amf amf = new Amf();
    }
}
