package org.eientei.videostreamer.mp4;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public abstract class Box {
    private final String atom;

    public Box(String atom) {
        this.atom = atom;
    }

    public final byte[] build() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeInt(0);
        out.write(atom.getBytes());

        write(out);

        byte[] data = out.toByteArray();
        int length = data.length;
        data[0] = (byte) ((length >> 24) & 0xFF);
        data[1] = (byte) ((length >> 16) & 0xFF);
        data[2] = (byte) ((length >> 8) & 0xFF);
        data[3] = (byte) (length & 0xFF);
        bake(data);
        return data;
    }

    protected void bake(byte[] data) {
    }

    protected abstract void write(ByteArrayDataOutput out);

    protected void writeMatrix(ByteArrayDataOutput out, int a, int b, int c, int d, int tx, int ty) {
        out.writeInt(a << 16);
        out.writeInt(b << 16);
        out.writeInt(0);

        out.writeInt(c << 16);
        out.writeInt(d << 16);
        out.writeInt(0);

        out.writeInt(tx << 16);
        out.writeInt(ty << 16);
        out.writeInt(1 << 30);
    }
}
