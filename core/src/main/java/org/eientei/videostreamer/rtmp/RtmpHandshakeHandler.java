package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public class RtmpHandshakeHandler extends ChannelInboundHandlerAdapter {
    private final static int HANDSHAKE_LENGTH = 1536;
    private final static byte[] CLIENT_KEY = {
            'G', 'e', 'n', 'u', 'i', 'n', 'e', ' ', 'A', 'd', 'o', 'b', 'e', ' ',
            'F', 'l', 'a', 's', 'h', ' ', 'P', 'l', 'a', 'y', 'e', 'r', ' ',
            '0', '0', '1',
            (byte) 0xF0, (byte) 0xEE, (byte) 0xC2, (byte) 0x4A, (byte) 0x80, (byte) 0x68, 
            (byte) 0xBE, (byte) 0xE8, (byte) 0x2E, (byte) 0x00, (byte) 0xD0, (byte) 0xD1, 
            (byte) 0x02, (byte) 0x9E, (byte) 0x7E, (byte) 0x57, (byte) 0x6E, (byte) 0xEC, 
            (byte) 0x5D, (byte) 0x2D, (byte) 0x29, (byte) 0x80, (byte) 0x6F, (byte) 0xAB, 
            (byte) 0x93, (byte) 0xB8, (byte) 0xE6, (byte) 0x36, (byte) 0xCF, (byte) 0xEB, 
            (byte) 0x31, (byte) 0xAE
    };
    
    private final static byte[] SERVER_KEY = {
            'G', 'e', 'n', 'u', 'i', 'n', 'e', ' ', 'A', 'd', 'o', 'b', 'e', ' ',
            'F', 'l', 'a', 's', 'h', ' ', 'M', 'e', 'd', 'i', 'a', ' ',
            'S', 'e', 'r', 'v', 'e', 'r', ' ',
            '0', '0', '1',
            (byte) 0xF0, (byte) 0xEE, (byte) 0xC2, (byte) 0x4A, (byte) 0x80, (byte) 0x68,
            (byte) 0xBE, (byte) 0xE8, (byte) 0x2E, (byte) 0x00, (byte) 0xD0, (byte) 0xD1,
            (byte) 0x02, (byte) 0x9E, (byte) 0x7E, (byte) 0x57, (byte) 0x6E, (byte) 0xEC,
            (byte) 0x5D, (byte) 0x2D, (byte) 0x29, (byte) 0x80, (byte) 0x6F, (byte) 0xAB,
            (byte) 0x93, (byte) 0xB8, (byte) 0xE6, (byte) 0x36, (byte) 0xCF, (byte) 0xEB,
            (byte) 0x31, (byte) 0xAE
    };


    private final static byte[] CLIENT_KEY_TEXT = Arrays.copyOf(CLIENT_KEY, 30);

    private final static byte[] SERVER_KEY_TEXT = Arrays.copyOf(SERVER_KEY, 36);

    private final static byte[] SERVER_VERSION = {
            (byte) 0x0D, (byte) 0x0E, (byte) 0x0A, (byte) 0x0D
    };

    private final Logger log = LoggerFactory.getLogger(RtmpHandshakeHandler.class);
    private final ByteBuf buf = Unpooled.buffer(HANDSHAKE_LENGTH+1, HANDSHAKE_LENGTH+1);
    private final Object mutex = new Object();
    private boolean complete;
    private int stage = 0;
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object raw) throws Exception {
        if (!(raw instanceof ByteBuf)) {
            ctx.fireChannelRead(raw);
            return;
        }

        ByteBuf msg = (ByteBuf) raw;

        synchronized (mutex) {
            if (complete) {
                return;
            }

            try {
                switch (stage) {
                    case 0:
                        stage0(ctx, msg);
                        break;
                    case 2:
                        stage2(ctx, msg);
                        break;
                }
                if (!complete) {
                    msg.release();
                }
            } catch (Exception e) {
                log.error("Error during handshake", e);
                complete = true;
                ctx.close();
                msg.release();
            }
        }
    }

    /* pre-condition: connection was opened for handshaking | handshake data is being read
     * post-condition: handshake data was read
     */
    private void stage0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (!readfull(msg)) {
            return;
        }

        // terminator
        stage1(ctx, msg);
    }

    /* pre-condition: handshake data was read
     * post-condition: handshake reply was written
     */
    private void stage1(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (buf.getByte(0) != 0x03) {
            throw new Exception("First byte of C0 was not 0x03");
        }

        byte[] data = new byte[HANDSHAKE_LENGTH];
        buf.getBytes(1, data);
        int roffs = findDigest(data, 772);
        if (roffs == -1) {
            roffs = findDigest(data, 8);
            if (roffs == -1) {
                throw new Exception("Digest was not found in C0");
            }
        }

        byte[] digest = makeDigest(Arrays.copyOfRange(data, roffs, roffs+32), SERVER_KEY, -1);
        System.arraycopy(SERVER_VERSION, 0, data, 4, SERVER_VERSION.length);
        byte[] random = new byte[data.length-8];
        Random randomgen = new Random();
        randomgen.nextBytes(random);
        System.arraycopy(random, 0, data, 8, random.length);
        int woffs = 0;
        for (int n = 8; n < 12; n++) {
            woffs += unsign(data[n]);
        }

        woffs = (woffs % 728) + 12;
        byte[] intermdigest = makeDigest(data, SERVER_KEY_TEXT, woffs);
        System.arraycopy(intermdigest, 0, data, woffs, intermdigest.length);

        buf.retain();
        buf.resetReaderIndex();
        buf.resetWriterIndex();
        buf.writeByte(0x03);
        buf.writeBytes(data);
        ctx.writeAndFlush(buf).sync();

        randomgen.nextBytes(data);
        System.arraycopy(makeDigest(data, digest, HANDSHAKE_LENGTH-32), 0, data, HANDSHAKE_LENGTH-32, 32);

        buf.retain();
        buf.resetReaderIndex();
        buf.resetWriterIndex();
        buf.writeByte(0x03);
        buf.writeBytes(data);
        buf.skipBytes(1);
        ctx.writeAndFlush(buf).sync();

        buf.resetWriterIndex();
        buf.writeByte(0x03);

        // terminator
        stage2(ctx, msg);
    }

    /* pre-condition: handshake reply is written | handhshake re-reply is being read
     * post-condition: handhsake re-reply was read and verified, hadshake is fully complete,
     *                 handshake handler was removed from pipeline
     */
    private void stage2(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        stage = 2;
        if (!readfull(msg)) {
            return;
        }

        // terminator
        complete = true;
        buf.release();
        msg.discardReadBytes();
        ctx.fireChannelRead(msg);
        ctx.pipeline().remove(this);
        ctx.channel().attr(RtmpServer.RTMP_CONNECTION_CONTEXT).get().open();
    }

    private boolean readfull(ByteBuf msg) {
        msg.readBytes(buf, Math.min(msg.readableBytes(), buf.writableBytes()));
        return !buf.isWritable();
    }


    private int findDigest(byte[] data, int mod) throws Exception {
        int offset = 0;
        for (int n = 0; n < 4; n++) {
            offset += unsign(data[mod+n]);
        }

        offset = (offset % 728) + mod + 4;
        byte[] digest = makeDigest(data, CLIENT_KEY_TEXT, offset);
        byte[] range = Arrays.copyOfRange(data, offset, offset+32);
        if (!Arrays.equals(range, digest)) {
            offset = -1;
        }
        return offset;
    }

    private int unsign(byte b) {
        return ((int)b << 24) >>> 24;
    }

    private byte[] makeDigest(byte[] data, byte[] key, int offset) throws Exception {
        Mac sha256hmac = Mac.getInstance("HmacSHA256");
        sha256hmac.init(new SecretKeySpec(key, "HmacSHA256"));
        if (offset >= 0 && offset < data.length) {
            if (offset != 0) {
                byte[] bs = Arrays.copyOfRange(data, 0, offset);
                sha256hmac.update(bs);
            }
            if (data.length != offset + 32) {
                byte[] bs = Arrays.copyOfRange(data, offset+32, data.length);
                sha256hmac.update(bs);
            }
        } else {
            sha256hmac.update(data);
        }
        return sha256hmac.doFinal();
    }

}
