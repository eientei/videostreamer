package org.eientei.videostreamer.mp4;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.h264.PpsNalUnit;
import org.eientei.videostreamer.h264.SliceNalUnit;
import org.eientei.videostreamer.h264.SpsNalUnit;
import org.eientei.videostreamer.amf.AmfObjectWrapper;
import org.eientei.videostreamer.mp4.boxes.Mp4Avc1Box;
import org.eientei.videostreamer.mp4.boxes.Mp4VmhdBox;

/**
 * Created by Alexander Tumin on 2016-10-22
 */
public class Mp4VideoTrackH264 extends Mp4Track {
    private static final int H264_SEI_SLICE = 6;
    private static final int H264_FILLER_SLICE = 12;
    private final ByteBuf avc;
    private final int confver;
    private final int profile;
    private final int profileCompat;
    private final int avcLevel;
    private final int nalBytes;
    private final SpsNalUnit sps;
    private final PpsNalUnit pps;
    private final Mp4Avc1Box init;
    private final Mp4VmhdBox mhd;
    private int lastFrameNum = -1;
    private ByteBuf lastFrameData;
    private boolean lastKeyframe;

    public Mp4VideoTrackH264(Mp4Context context, AmfObjectWrapper metadata, ByteBuf videoro) {
        super(context, 0,
                ((Number)metadata.get("width")).intValue(),
                ((Number)metadata.get("height")).intValue(),
                ((Number)metadata.get("fps")).intValue(), 1);



        avc = videoro.copy(videoro.readerIndex(), videoro.readableBytes());
        confver = videoro.readUnsignedByte();
        profile = videoro.readUnsignedByte();
        profileCompat = videoro.readUnsignedByte();
        avcLevel = videoro.readUnsignedByte();
        nalBytes = (videoro.readUnsignedByte() & 0x03) + 1;
        videoro.skipBytes(1); // sps count should be equal to 1
        int spslength = videoro.readUnsignedShort();
        sps = new SpsNalUnit(videoro, spslength);
        videoro.skipBytes(spslength);
        videoro.skipBytes(1); // pps count should be equal to 1
        int ppslength = videoro.readUnsignedShort();
        pps = new PpsNalUnit(videoro, ppslength);
        init = new Mp4Avc1Box(context, this);
        mhd = new Mp4VmhdBox(context);
        lastFrameData = context.ALLOC.allocSizeless();
    }

    @Override
    public void update(ByteBuf readonly, boolean keyframe) {

        while (readonly.isReadable()) {
            int size = 0;
            switch (nalBytes) {
                case 1:
                    size = readonly.readUnsignedByte();
                    break;
                case 2:
                    size = readonly.readUnsignedShort();
                    break;
                case 3:
                    size = readonly.readUnsignedMedium();
                    break;
                case 4:
                    size = (int) readonly.readUnsignedInt();
                    break;
            }

            SliceNalUnit slice = new SliceNalUnit(sps, readonly, size);
            if (slice.nalUnitType == H264_SEI_SLICE || slice.nalUnitType == H264_FILLER_SLICE) {
                readonly.skipBytes(size);
                continue;
            }

            if (lastFrameNum != -1 && lastFrameNum != slice.frameNum) {
                addSample(new Mp4Sample(lastFrameData.copy(), lastKeyframe));
                lastFrameData.release();
                lastKeyframe = false;
                lastFrameData = getContext().ALLOC.allocSizeless();
            }
            lastKeyframe = lastKeyframe || keyframe;
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
            lastFrameData.writeBytes(readonly, size);
            lastFrameNum = slice.frameNum;
        }
    }

    @Override
    public void release() {
        avc.release();
        lastFrameData.release();
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
    public Mp4Box getInit() {
        return init;
    }

    @Override
    public Mp4Box getMhd() {
        return mhd;
    }

    public ByteBuf getAvc() {
        return avc;
    }
}
