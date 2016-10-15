package org.eientei.videostreamer.rtmp;

/**
 * Created by Alexander Tumin on 2016-10-12
 */
public class RtmpHeader {
    private final RtmpMessageType type;
    private final int chunkid;
    private final long streamid;
    private final long time;

    public RtmpHeader(RtmpMessageType type, int chunkid, long streamid, long time) {
        this.type = type;
        this.chunkid = chunkid;
        this.streamid = streamid;
        this.time = time;
    }

    public RtmpMessageType getType() {
        return type;
    }

    public int getChunkid() {
        return chunkid;
    }

    public long getStreamid() {
        return streamid;
    }

    public long getTime() {
        return time;
    }
}