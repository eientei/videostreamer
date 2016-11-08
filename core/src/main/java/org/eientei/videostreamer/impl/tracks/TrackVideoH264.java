package org.eientei.videostreamer.impl.tracks;

import com.github.jinahya.bit.io.BitInput;
import com.github.jinahya.bit.io.DefaultBitInput;
import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.impl.core.Message;
import org.eientei.videostreamer.impl.core.Sample;
import org.eientei.videostreamer.impl.core.Track;
import org.eientei.videostreamer.impl.util.ByteBufInput;

import java.io.IOException;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class TrackVideoH264 extends Track implements TrackVideo {
    private final static int H264_SEI_SLICE = 6;
    private final static int H264_FILLER_SLICE = 12;

    private final int nalLenlen;
    private final int frameNumbits;
    private final Message init;
    private final int width;
    private final int height;

    public TrackVideoH264(Message video, int fps, int width, int height) throws IOException {
        super(fps, 1);
        this.width = width;
        this.height = height;
        video.retain();
        init = video;

        ByteBuf data = video.getData();

        data.skipBytes(5);

        data.skipBytes(4);
        nalLenlen = (data.readUnsignedByte() & 0x03) + 1;
        data.skipBytes(6);
        BitInput bit = new DefaultBitInput<>(new ByteBufInput(data.slice()));
        skipUE(bit);
        frameNumbits = parseUE(bit) + 4;
    }

    private int parseUE(BitInput data) throws IOException {
        int n = readN0(data);
        if (n == 0) {
            return 0;
        }
        return (int) (Math.pow(2, n) - 1 + data.readInt(true, n));
    }

    private int parseInt(BitInput data, int n) throws IOException {
        return data.readInt(true, n);
    }

    private int readN0(BitInput data) throws IOException {
        int n = -1;
        for (int b = 0; b == 0; n++) {
            b = data.readInt(true, 1);
        }
        return n;
    }

    private void skipUE(BitInput data) throws IOException {
        parseUE(data);
    }

    public int getNalLenlen() {
        return nalLenlen;
    }

    public int getFrameNumbits() {
        return frameNumbits;
    }

    public ByteBuf getInit() {
        return init.getData().skipBytes(5);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    protected void deallocate() {
        init.release();
    }

    @Override
    public Sample makeSample(Message message) throws Exception {
        ByteBuf data = message.getData();
        data.skipBytes(5);

        int begin = data.readerIndex();
        int length = 0;
        while (data.isReadable()) {
            int size = 0;
            switch (nalLenlen) {
                case 1:
                    size = data.readUnsignedByte();
                    break;
                case 2:
                    size = data.readUnsignedShort();
                    break;
                case 3:
                    size = data.readUnsignedMedium();
                    break;
                case 4:
                    size = (int) data.readUnsignedInt();
                    break;
            }

            BitInput bit = new DefaultBitInput<>(new ByteBufInput(data.slice()));
            int forbiddenZeroBit = parseInt(bit, 1);
            int nalRefIdc = parseInt(bit, 2);
            int nalUnitType = parseInt(bit, 5);
            /*int firstMbInSlice = parseUE(bit);*/
            /*int sliceType = parseUE(bit);*/
            /*int frameNum = parseInt(bit, frameNumbits);*/

            if (nalUnitType == H264_FILLER_SLICE) {
                data.skipBytes(data.readableBytes());
                continue;
            }

            data.skipBytes(size);
            length += nalLenlen + size;
        }
        return new Sample(message, begin, length);
    }
}
