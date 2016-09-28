package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public class RtmpMessageAssembler {
    private RtmpUnchunkedMessage current;
    private RtmpHeader header;
    private long timeDiff = 0;


    public RtmpMessageAssembler(int chunkid) {
        header = new RtmpHeader(chunkid, 0, RtmpMessage.Type.UNKNOWN0);
    }
    public void reset() {
        current.release();
        current = null;
    }

    public void nextChunk(RtmpHeader.Size size, ByteBuf in) {
        parseHeader(size, in);

        if (current == null) {
            current = new RtmpUnchunkedMessage(header);
        }
    }

    private void parseHeader(RtmpHeader.Size size, ByteBuf in) {
        switch (size) {
            case NONE:
                if (current == null) {
                    header.setTimestamp(header.getTimestamp() + timeDiff);
                }
                break;
            case SHORT:
                timeDiff = in.readUnsignedMedium();
                header.setTimestamp(header.getTimestamp() + timeDiff);
                break;
            case MEDIUM:
                timeDiff = in.readUnsignedMedium();
                header.setTimestamp(header.getTimestamp() + timeDiff);
                header.setLength(in.readUnsignedMedium());
                header.setType(RtmpMessage.Type.parseValue(in.readUnsignedByte()));
                break;
            case FULL:
                header.setTimestamp(in.readUnsignedMedium());
                header.setLength(in.readUnsignedMedium());
                header.setType(RtmpMessage.Type.parseValue(in.readUnsignedByte()));
                header.setStreamid(in.readUnsignedIntLE());
                if (header.getTimestamp() == 0xFFFFFF) {
                    header.setTimestamp(in.readUnsignedInt());
                }
                break;
        }
    }

    public RtmpUnchunkedMessage getCurrent() {
        return current;
    }
}
