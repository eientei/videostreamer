package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class FtypBox extends Box {
    public FtypBox() {
        super("ftyp");
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.write("iso6".getBytes());
        out.writeInt(1);
        out.write("isom".getBytes());
        out.write("iso6".getBytes());
    }
}
