package org.eientei.videostreamer.mvc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.eientei.videostreamer.impl.core.BinaryFrame;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Alexander Tumin on 2016-11-07
 */
public class ChunkedOutputHandler extends ChannelOutboundHandlerAdapter {
    private final OutputStream outputStream;
    private final EmbeddedChannel embed;
    private int audioTime = 0;
    private int videoTime = 0;
    private ByteBuf init;

    public ChunkedOutputHandler(EmbeddedChannel embed, OutputStream outputStream) {
        this.embed = embed;
        this.outputStream = outputStream;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf init = (ByteBuf) msg;
            try {
                this.init = init;
                //outputStream.flush();
            } catch (Throwable t) {
                //t.printStackTrace();
                embed.close();
                outputStream.close();
                ctx.close();
            } finally {
                //init.release();
            }
            return;
        }
        BinaryFrame binaryFrame = (BinaryFrame) msg;
        if (init != null) {
            if (!binaryFrame.isKey()) {
                binaryFrame.release();
                return;
            }
        }
        try {
            if (init != null) {
                writeOut(init);
                outputStream.flush();
                init.release();
                init = null;
            }
            writeOut(binaryFrame);
            outputStream.flush();
            audioTime += binaryFrame.getAudioAdvance();
            videoTime += binaryFrame.getVideoAdvance();
        } catch (Throwable t) {
            //t.printStackTrace();
            embed.close();
            outputStream.close();
            ctx.close();
        } finally {
            binaryFrame.release();
        }
    }

    private void writeOut(BinaryFrame binaryFrame) throws IOException {
        writeOut(binaryFrame.getP1());
        writeOut(binaryFrame.getAudioTime(audioTime));
        writeOut(binaryFrame.getP2());
        writeOut(binaryFrame.getVideoTime(videoTime));
        writeOut(binaryFrame.getP3());
    }

    private void writeOut(int time) throws IOException {
        outputStream.write((time >> 24 & 0xFF));
        outputStream.write((time >> 16 & 0xFF));
        outputStream.write((time >> 8 & 0xFF));
        outputStream.write((time & 0xFF));
    }

    private void writeOut(ByteBuf buf) throws IOException {
        outputStream.write(buf.array(), buf.arrayOffset()+buf.readerIndex(), buf.readableBytes());
    }
}
