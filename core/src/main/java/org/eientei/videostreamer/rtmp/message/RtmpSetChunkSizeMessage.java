package org.eientei.videostreamer.rtmp.message;

import io.netty.buffer.ByteBuf;
import org.eientei.videostreamer.rtmp.RtmpMessage;
import org.eientei.videostreamer.rtmp.RtmpMessageParser;
import org.eientei.videostreamer.rtmp.RtmpUnchunkedMessage;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public class RtmpSetChunkSizeMessage extends RtmpMessage {
    public static final RtmpMessageParser<RtmpSetChunkSizeMessage> PARSER = new RtmpMessageParser<RtmpSetChunkSizeMessage>() {
        @Override
        public RtmpSetChunkSizeMessage parse(RtmpUnchunkedMessage msg) {
            long chunksize = msg.getData().readUnsignedInt();
            return new RtmpSetChunkSizeMessage(chunksize);
        }
    };

    private final long chunkSize;

    public RtmpSetChunkSizeMessage(long chunkSize) {
        super(2, 0, Type.SET_CHUNK_SIZE);
        this.chunkSize = chunkSize;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    @Override
    public void serialize(ByteBuf data) {
        data.writeInt((int)getChunkSize());
    }
}
