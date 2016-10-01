package org.eientei.videostreamer.rtmp;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public class RtmpHeader {

    public enum Size {
        FULL(0),
        MEDIUM(1),
        SHORT(2),
        NONE(3);

        private final int value;

        Size(int i) {
            this.value = i;
        }

        public int getValue() {
            return value;
        }

        public static Size parseValue(int value) {
            if (value >= 0 && value < values().length) {
                return values()[value];
            }
            throw new IllegalArgumentException("Illegal RtmpHeader.Size value: " + value);
        }
    }

    private int chunkid;
    private RtmpMessage.Type type;
    private long timestamp;
    private int length;
    private long streamid;
    private Size forceSize = null;
    private long timeDiff;

    public void setTimeDiff(long timeDiff) {
        this.timeDiff = timeDiff;
    }

    public long getTimeDiff() {
        return timeDiff;
    }

    public RtmpHeader(int chunkid) {
        this.chunkid = chunkid;
    }

    public RtmpHeader(int chunkid, long streamid, RtmpMessage.Type type) {
        this.chunkid = chunkid;
        this.type = type;
        this.streamid = streamid;
    }

    public void setFrom(RtmpHeader header) {
        this.chunkid = header.getChunkid();
        this.timestamp = header.getTimestamp();
        this.length = header.getLength();
        this.type = header.getType();
        this.streamid = header.getStreamid();
        this.timeDiff = header.getTimeDiff();
    }

    public int getChunkid() {
        return chunkid;
    }

    public void setChunkid(int chunkid) {
        this.chunkid = chunkid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public RtmpMessage.Type getType() {
        return type;
    }

    public void setType(RtmpMessage.Type type) {
        this.type = type;
    }

    public long getStreamid() {
        return streamid;
    }

    public void setStreamid(long streamid) {
        this.streamid = streamid;
    }

    public Size getForceSize() {
        return forceSize;
    }

    public void setForceSize(Size forceSize) {
        this.forceSize = forceSize;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] <" + chunkid + "," + streamid + "> " + type + " " + length;
    }
}
