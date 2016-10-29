package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.amf.AmfObjectWrapper;
import org.eientei.videostreamer.h264.SliceNalUnit;
import org.eientei.videostreamer.h264.SpsNalUnit;
import org.eientei.videostreamer.mp4.boxes.Mp4Avc1Box;
import org.eientei.videostreamer.mp4.boxes.Mp4VmhdBox;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
public class Mp4VideoTrackH264 extends Mp4Track {
    private static final int H264_SEI_SLICE = 6;
    private static final int H264_FILLER_SLICE = 12;

    private final Mp4Avc1Box init;
    private final Mp4VmhdBox mhd;
    private final ByteBuf avc;
    private final int nalBytes;
    private final SpsNalUnit sps;
    private int lastFrameNum = -1;
    private ByteBuf lastFrameData;
    private boolean lastKeyframe;

    public Mp4VideoTrackH264(Mp4RemuxerHandler context, AmfObjectWrapper metadata, ByteBuf videoro) {
        super(
                0,
                ((Number)metadata.get("width")).intValue(),
                ((Number)metadata.get("height")).intValue(),
                1000,
                1000 / ((Number)metadata.get("fps")).intValue()
        );
        int idx = videoro.readerIndex();
        avc = videoro.copy();
        videoro.skipBytes(4);
        nalBytes = (videoro.readUnsignedByte() & 0x03) + 1;

        videoro.skipBytes(1); // sps count should be equal to 1
        int spslength = videoro.readUnsignedShort();
        sps = new SpsNalUnit(videoro);


        lastFrameData = videoro.alloc().buffer();
        init = new Mp4Avc1Box(context, this);
        mhd = new Mp4VmhdBox(context);
    }

    @Override
    public void update(ByteBuf data, int timestamp, boolean isKeyframe) {
        while (data.isReadable()) {
            try {
                int size = 0;
                switch (nalBytes) {
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

                SliceNalUnit slice = new SliceNalUnit(sps, data.slice());
                if (slice.nalUnitType == H264_SEI_SLICE || slice.nalUnitType == H264_FILLER_SLICE) {
                    data.skipBytes(size);
                    continue;
                }

                if (lastFrameNum != -1 && lastFrameNum != slice.frameNum) {
                    addSample(new Mp4Sample(lastFrameData.copy(), lastKeyframe, timestamp, getFrametick()));
                    lastKeyframe = false;
                    lastFrameData.resetWriterIndex();
                }
                lastKeyframe = lastKeyframe || isKeyframe;
                switch (nalBytes) {
                    case 1:
                        lastFrameData.writeByte(size);
                        break;
                    case 2:
                        lastFrameData.writeShort(size);
                        break;
                    case 3:
                        lastFrameData.writeMedium(size);
                        break;
                    case 4:
                        lastFrameData.writeInt(size);
                        break;
                }

                lastFrameData.writeBytes(data, size);
                lastFrameNum = slice.frameNum;
            } catch (Exception ignore) {

            }
        }
    }

    @Override
    public String getShortHandler() {
        return "vide";
    }

    @Override
    public String getLongHandler() {
        return "Video Handler";
    }

    @Override
    public Mp4Box getInit(List<Mp4Track> tracks) {
        return init;
    }

    @Override
    public Mp4Box get_Mhd() {
        return mhd;
    }

    @Override
    public void release() {
        avc.release();
        lastFrameData.release();
        for (Mp4Sample sample : drainSamples()) {
            sample.release();
        }
    }

    public ByteBuf getAvc() {
        return avc.slice();
    }
}
