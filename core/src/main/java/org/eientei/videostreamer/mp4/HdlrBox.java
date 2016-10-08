package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class HdlrBox extends Box {
    private final Type type;

    public HdlrBox(Type type) {
        super("hdlr");
        this.type = type;
    }

    @Override
    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0); // version and flags
        out.writeInt(0); // predefined
        switch (type) {
            case VIDEO:
                out.write("vide".getBytes());
                break;
            case AUDIO:
                out.write("soun".getBytes());
                break;
        }
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        out.writeInt(0); // reserved
        switch (type) {
            case VIDEO:
                out.write("VideoHandler".getBytes());
                out.write(0);
                break;
            case AUDIO:
                out.write("SoundHandler".getBytes());
                out.write(0);
                break;
        }
    }
}
