package org.eientei.videostreamer.rtmp.message;

import org.eientei.videostreamer.rtmp.RtmpMessageParser;
import org.eientei.videostreamer.rtmp.RtmpUnchunkedMessage;

import java.util.List;

/**
 * Created by Alexander Tumin on 2016-09-25
 */
public class RtmpAmf0CmdMessage extends RtmpAmfCmdMessage {
    public static final RtmpMessageParser<RtmpAmf0CmdMessage> PARSER = new RtmpMessageParser<RtmpAmf0CmdMessage>() {
        @Override
        public RtmpAmf0CmdMessage parse(RtmpUnchunkedMessage msg) {
            return new RtmpAmf0CmdMessage(RtmpAmfCmdMessage.parseValues(msg));
        }
    };

    public RtmpAmf0CmdMessage(List<Object> values) {
        super(values, Type.AMF0_CMD);
    }
}
