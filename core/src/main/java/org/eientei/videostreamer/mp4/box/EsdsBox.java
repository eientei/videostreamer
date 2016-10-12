package org.eientei.videostreamer.mp4.box;

import com.google.common.io.ByteArrayDataOutput;
import org.eientei.videostreamer.mp4.util.MetaData;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class EsdsBox {
    private final MetaData meta;

    public EsdsBox(MetaData meta) {
    //    super("esds");
        this.meta = meta;
    }

    private void putDescr(ByteArrayDataOutput out, int tag, int size) {
        out.write(tag);
        out.write(size & 0x7f);
    }

    protected void write(ByteArrayDataOutput out) {
        out.writeInt(0); // version
        putDescr(out, 0x03, 23 + meta.getAudioDsi().length); // es descriptor
        out.writeShort(1); // es id
        out.write(0); // flags
        putDescr(out, 0x04, 15 + meta.getAudioDsi().length); // decoder config descriptor
        out.write(0x40); // aac
        out.write(0x15); // audio stream
        out.writeShort(0); out.write(0); // buffer size (24 bits)
        out.writeInt(0x0001F151); // max bitrate
        out.writeInt(0x0001F14D); // avg bitrate
        putDescr(out, 0x05, meta.getAudioDsi().length); // decoder specific info length
        out.write(meta.getAudioDsi()); // decoder specific info
        putDescr(out, 0x06, 1);  // sl descriptor
        out.write(0x02); // sl descriptor
    }
}
