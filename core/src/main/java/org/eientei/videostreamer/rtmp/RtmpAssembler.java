package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.eientei.videostreamer.rtmp.message.*;
import org.eientei.videostreamer.rtmp.server.RtmpMessageDecoder;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public class RtmpAssembler {
    private final RtmpMessageDecoder decoder;
    private final int chunkid;

    private RtmpMessageType type;
    private long streamid;
    private long time;

    private int length;
    private long timediff;
    private ByteBuf assembly = Unpooled.buffer();

    public RtmpAssembler(RtmpMessageDecoder decoder, int chunkid) {
        this.decoder = decoder;
        this.chunkid = chunkid;
    }

    public void update(RtmpHeaderType htype, ByteBuf in, List<Object> out) {
        switch (htype) {
            case FULL:
                time = in.readUnsignedMedium();
                length = in.readUnsignedMedium();
                type = RtmpMessageType.dispatch(in.readUnsignedByte());
                streamid = in.readUnsignedIntLE();
                if (time == 0xFFFFFF) {
                    time = in.readUnsignedInt();
                }
                break;
            case MEDIUM:
                timediff = in.readUnsignedMedium();
                time += timediff;
                length = in.readUnsignedMedium();
                type = RtmpMessageType.dispatch(in.readUnsignedByte());
                break;
            case SHORT:
                timediff = in.readUnsignedMedium();
                time += timediff;
                break;
            case NONE:
                if (!assembly.isReadable()) {
                    time += timediff;
                }
                break;
        }

        int remain =  length - assembly.readableBytes();
        if (remain > decoder.getChunksize()) {
            remain = decoder.getChunksize();
        }
        assembly.ensureWritable(remain);
        in.readBytes(assembly, assembly.writerIndex(), remain);
        assembly.writerIndex(assembly.writerIndex() + remain);
        if (assembly.readableBytes() == length) {
            switch (type) {
                case SET_CHUNK_SIZE:
                    RtmpSetChunkSizeMessage setchunk = new RtmpSetChunkSizeMessage(chunkid, streamid, time, assembly.copy());
                    decoder.setChunksize(setchunk.getChunkSize());
                    out.add(setchunk);
                    break;
                case ACK:
                    out.add(new RtmpAckMessage(chunkid, streamid, time, assembly.copy()));
                    break;
                case USER:
                    out.add(new RtmpUserMessage(chunkid, streamid, time, assembly.copy()));
                    break;
                case WINACK:
                    RtmpWinackMessage winack = new RtmpWinackMessage(chunkid, streamid, time, assembly.copy());
                    decoder.setAckwindow(winack.getSize());
                    out.add(winack);
                    break;
                case SET_PEER_BAND:
                    out.add(new RtmpSetPeerBandMessage(chunkid, streamid, time, assembly.copy()));
                    break;
                case AUDIO:
                    out.add(new RtmpAudioMessage(chunkid, streamid, time, assembly.copy()));
                    break;
                case VIDEO:
                    out.add(new RtmpVideoMessage(chunkid, streamid, time, assembly.copy()));
                    break;
                case AMF3_CMD_ALT:
                    out.add(new RtmpAmfMessage(type, chunkid, streamid, time, assembly.copy(1, assembly.readableBytes()-1)));
                    break;
                case AMF3_META:
                    out.add(new RtmpAmfMessage(type, chunkid, streamid, time, assembly.copy(1, assembly.readableBytes()-1)));
                    break;
                case AMF3_CMD:
                    out.add(new RtmpAmfMessage(type, chunkid, streamid, time, assembly.copy(1, assembly.readableBytes()-1)));
                    break;
                case AMF0_META:
                    out.add(new RtmpAmfMessage(type, chunkid, streamid, time, assembly.copy()));
                    break;
                case AMF0_CMD:
                    out.add(new RtmpAmfMessage(type, chunkid, streamid, time, assembly.copy()));
                    break;
            }
            assembly.resetWriterIndex();
            decoder.addReadcount(length);
        }
    }

    public void release() {
        assembly.release();
    }
}
