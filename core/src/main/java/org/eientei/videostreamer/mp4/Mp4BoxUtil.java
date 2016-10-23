package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4BoxUtil {
    public static void writeMatrix(ByteBuf out, int a, int b, int c, int d, int tx, int ty) {
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
