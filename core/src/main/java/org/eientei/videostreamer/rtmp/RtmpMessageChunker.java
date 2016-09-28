package org.eientei.videostreamer.rtmp;

/**
 * Created by Alexander Tumin on 2016-09-27
 */
public class RtmpMessageChunker {
    private RtmpHeader header;
    private long timeDiff = 0;

    public RtmpMessageChunker(int chunkid) {
        header = new RtmpHeader(chunkid);
    }

    public RtmpHeader getHeader() {
        return header;
    }

    public long getTimeDiff() {
        return timeDiff;
    }

    public void setTimeDiff(long timeDiff) {
        this.timeDiff = timeDiff;
    }
}
