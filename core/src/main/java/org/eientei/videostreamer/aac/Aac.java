package org.eientei.videostreamer.aac;

import com.github.jinahya.bit.io.BufferByteInput;
import com.github.jinahya.bit.io.DefaultBitInput;
import org.eientei.videostreamer.util.BitParser;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class Aac extends BitParser {
    private static int[] aacSampleRates = new int[] {
            96000, 88200, 64000, 48000,
            44100, 32000, 24000, 22050,
            16000, 12000, 11025,  8000,
            7350,      0,     0,     0
    };

    public final int aacProfile;
    public final int sampleRate;
    public final int aacChainConf;
    public final int aacPs;
    public final int aacSbr;
    public final byte[] buf;

    public Aac(ByteBuffer wrap) throws IOException {
        super(new DefaultBitInput<>(new BufferByteInput(wrap)));
        parseInt(16);
        buf = new byte[wrap.remaining()];
        wrap.slice().get(buf);

        int prof = parseInt(5);
        if (prof == 31) {
            prof = parseInt(6) + 32;
        }
        int idx = parseInt(4);
        int sampler;
        if (idx == 15) {
            sampler = parseInt(24);
        } else {
            sampler = aacSampleRates[idx];
        }

        aacChainConf = parseInt(4);
        if (prof == 5 || prof == 29) {
            if (prof == 29) {
                aacPs = 1;
            } else {
                aacPs = 0;
            }

            aacSbr = 1;

            idx = parseInt(4);
            if (idx == 15) {
                sampler = parseInt(24);
            } else {
                sampler = aacSampleRates[idx];
            }

            prof = parseInt(5);
            if (prof == 31) {
                prof = parseInt(6) + 32;
            }
        } else {
            aacPs = 0;
            aacSbr = 0;
        }

        aacProfile = prof;
        sampleRate = sampler;
    }
}
