package org.eientei.videostreamer.mvc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.eientei.videostreamer.impl.core.BinaryFrame;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Alexander Tumin on 2016-11-07
 */
public class ChunkedOutputHandler extends ChannelOutboundHandlerAdapter {
    private final OutputStream outputStream;
    private int audioTime = 0;
    private int videoTime = 0;

    public ChunkedOutputHandler(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            try {
                writeOut(buf);
                outputStream.flush();
            } catch (Throwable t) {
                ctx.close();
            } finally {
                buf.release();
            }
            return;
        }
        BinaryFrame binaryFrame = (BinaryFrame) msg;
        try {
            writeOut(binaryFrame.getP1());
            writeOut(binaryFrame.getAudioTime(audioTime));
            writeOut(binaryFrame.getP2());
            writeOut(binaryFrame.getVideoTime(videoTime));
            writeOut(binaryFrame.getP3());
            outputStream.flush();
            audioTime += binaryFrame.getAudioAdvance();
            videoTime += binaryFrame.getVideoAdvance();
        } catch (Throwable t) {
            ctx.close();
        } finally {
            binaryFrame.release();
        }
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

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        outputStream.close();
    }
}
