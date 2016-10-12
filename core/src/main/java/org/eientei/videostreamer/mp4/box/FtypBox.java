package org.eientei.videostreamer.mp4.box;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.mp4.util.BoxContext;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class FtypBox extends Box {
    private final String brand;
    private final int version;
    private final String[] supbrands;

    public FtypBox(BoxContext context, String brand, int version, String... supbrands) {
        super("ftyp", context);
        this.brand = brand;
        this.version = version;
        this.supbrands = supbrands;
    }

    @Override
    protected void doWrite(ByteBuf out) {
        out.writeBytes(brand.getBytes());
        out.writeInt(version);
        for (String b : supbrands) {
            out.writeBytes(b.getBytes());
        }
    }
}
