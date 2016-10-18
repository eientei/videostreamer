package org.eientei.videostreamer.mp3;

import com.github.jinahya.bit.io.ArrayByteInput;
import com.github.jinahya.bit.io.DefaultBitInput;
import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.util.BitParser;

/**
 * Created by Alexander Tumin on 2016-10-16
 */
public class Mp3Header extends BitParser {
    public final int syncword;
    public final int mpegversion;
    public final int layer;
    public final int noprotection;
    public final int bitrate;
    public final int samplerate;
    public final int padding;
    public final int privbit;
    public final int channelmode;
    public final int joint;
    public final int copyk;
    public final int copys;
    public final int emphasis;

    public Mp3Header(ByteBuf data) {
        super(new DefaultBitInput<>(new ArrayByteInput(data.array(), data.readerIndex(), data.readableBytes())));
        syncword = parseInt(11);
        mpegversion = parseInt(2);
        layer = parseInt(2);
        noprotection = parseInt(1);
        bitrate = parseInt(4);
        samplerate = parseInt(2);
        padding = parseInt(1);
        privbit = parseInt(1);
        channelmode = parseInt(2);
        joint = parseInt(2);
        copyk = parseInt(1);
        copys = parseInt(1);
        emphasis = parseInt(2);
    }
}
