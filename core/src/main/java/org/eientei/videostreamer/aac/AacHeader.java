package org.eientei.videostreamer.aac;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-10-15
 */
public class AacHeader {
    public final int objectType;
    public final int sampleRateIdx;
    public final int sampleRate;
    public final int[] rates = new int[] {96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000};
    public final int channelConf;
    public final int frameLenFlag;
    public final int dependsOnCoreCoder;
    public final int extensionFlag;

    public AacHeader(ByteBuf data) {
        int a = data.readUnsignedByte();
        int b = data.readUnsignedByte();

        objectType = (a & 0b11111000) >> 3;
        sampleRateIdx = ((a & 0b00000111) << 1) | ((b & 0b10000000) >> 7);
        if (sampleRateIdx == 15) {
            sampleRate = -1; //parseInt(24);
        } else {
            sampleRate = rates[sampleRateIdx];
        }
        channelConf = (b & 0b01111000) >> 3;
        frameLenFlag = (b & 0b00000100) >> 2;
        dependsOnCoreCoder = (b & 0b00000010) >> 1;
        extensionFlag = b & 0b00000001;
    }
}
