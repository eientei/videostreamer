package org.eientei.videostreamer.rtmp.message;

import org.eientei.videostreamer.rtmp.RtmpMessageParser;
import org.eientei.videostreamer.rtmp.RtmpUnchunkedMessage;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-09-28
 */
public class RtmpAmf3CmdMessage extends RtmpAmfCmdMessage {
    public static final RtmpMessageParser<RtmpAmf3CmdMessage> PARSER = new RtmpMessageParser<RtmpAmf3CmdMessage>() {
        @Override
        public RtmpAmf3CmdMessage parse(RtmpUnchunkedMessage msg) {
            msg.getData().skipBytes(1);
            return new RtmpAmf3CmdMessage(RtmpAmfCmdMessage.parseValues(msg));
        }
    };
    public RtmpAmf3CmdMessage(List<Object> values) {
        super(values, Type.AMF3_CMD);
    }
}
