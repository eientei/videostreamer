package org.eientei.videostreamer.ws;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created by Alexander Tumin on 2016-10-29
 */
public class WebsocketDecoderHandler extends SimpleChannelInboundHandler<String> {
    private final JsonFactory jsonFactory = new JsonFactory();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        JsonParser parser = jsonFactory.createParser(msg);
        parser.nextToken();
        String action = parser.nextFieldName();
        String params = parser.nextTextValue();
        ctx.fireChannelRead(new WebsocketTextQuery(action, params));
    }
}
