package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by Alexander Tumin on 2016-09-24
 */
public class RtmpHandshakeHandler extends ReplayingDecoder<RtmpHandshakeHandler.Stage>{

    public enum Stage {
        STAGE0,
        STAGE1,
        STAGE2
    }

    private Logger log = LoggerFactory.getLogger(RtmpHandshakeHandler.class);
    private final static int HANDSHAKE_DIGEST_LENGTH = 32;
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

    private final byte[] buf = new byte[HANDSHAKE_LENGTH];
    private long epoch = 0;

    public RtmpHandshakeHandler() {
        super(Stage.STAGE0);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf bmsg, List<Object> out) throws Exception {
        switch (state()) {
            case STAGE0:
                stage0(ctx, bmsg);
                break;
            case STAGE1:
                stage1(ctx, bmsg);
                break;
            case STAGE2:
                stage2(ctx, bmsg);
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        RtmpClientContext client = ctx.channel().attr(RtmpServer.RTMP_CLIENT_CONTEXT).get();;
        log.info("Client {} failed handshake", client.getId());
        ctx.close();
    }

    private void stage2(ChannelHandlerContext ctx, ByteBuf bmsg) {
        bmsg.readBytes(buf);
        RtmpClientContext client = ctx.channel().attr(RtmpServer.RTMP_CLIENT_CONTEXT).get();
        log.info("Client {} connected", client.getId());
        ctx.pipeline().remove(this);
    }

    private void stage1(ChannelHandlerContext ctx, ByteBuf bmsg) throws Exception {
        epoch = bmsg.getUnsignedInt(0);
        bmsg.readBytes(buf);

        int offset = findDigest(772, CLIENT_KEY_TEXT);
        if (offset == -1) {
            offset = findDigest(8, CLIENT_KEY_TEXT);
        }

        byte[] digest = null;
        byte[] oldbuf = null;
        if (offset != -1) {
            byte[] slice = Arrays.copyOfRange(buf, offset, offset + HANDSHAKE_DIGEST_LENGTH);
            digest = makeDigest(slice, -1, SERVER_KEY);
        } else {
            oldbuf = Arrays.copyOf(buf, buf.length);
        }


        for (int i = 0; i < 4; i++) {
            buf[i] = 0;
        }
        System.arraycopy(SERVER_VERSION, 0, buf, 4, 4);
        randomize(buf, 8);
        writeDigest(buf, SERVER_KEY_TEXT);

        ctx.writeAndFlush(Unpooled.wrappedBuffer(new byte[] { 0x03 })).sync();
        ctx.writeAndFlush(Unpooled.wrappedBuffer(buf)).sync();

        if (digest != null) {
            randomize(buf, 8);
            digest = makeDigest(buf, HANDSHAKE_LENGTH - HANDSHAKE_DIGEST_LENGTH, digest);
            System.arraycopy(digest, 0, buf, HANDSHAKE_LENGTH - HANDSHAKE_DIGEST_LENGTH, HANDSHAKE_DIGEST_LENGTH);
            ctx.writeAndFlush(Unpooled.wrappedBuffer(buf)).sync();
        }

        if (oldbuf != null) {
            ctx.writeAndFlush(Unpooled.wrappedBuffer(oldbuf)).sync();
        }

        state(Stage.STAGE2);
    }

    private void randomize(byte[] buf, int from) {
        Random random = new Random();
        for (int i = from; i < buf.length; i++) {
            buf[i] = (byte) random.nextInt(8);
        }
    }

    private void writeDigest(byte[] buf, byte[] key) throws Exception {
        int offset = 0;
        for (int n = 8; n < 12; n++) {
            offset += getUnsignedByte(buf, n);
        }
        offset = (offset % 728) + 12;
        byte[] digest = makeDigest(buf, offset, key);
        System.arraycopy(digest, 0, buf, offset, HANDSHAKE_DIGEST_LENGTH);
    }

    private int findDigest(int base, byte[] peerKey) throws Exception {
        int offset = 0;
        for (int n = 0; n < 4; n++) {
            offset += getUnsignedByte(buf, base+n);
        }

        offset = (offset % 728) + base + 4;
        byte[] digest = makeDigest(buf, offset, peerKey);
        byte[] reference = new byte[HANDSHAKE_DIGEST_LENGTH];
        System.arraycopy(buf, offset, reference, 0, HANDSHAKE_DIGEST_LENGTH);
        if (Arrays.equals(digest, reference)) {
            return offset;
        }
        return -1;
    }

    private int getUnsignedByte(byte[] buf, int i) {
        return buf[i] & 0xFF;
    }

    private byte[] makeDigest(byte[] data, int offset, byte[] peerKey) throws Exception {
        Mac sha256hmac = Mac.getInstance("HmacSHA256");
        sha256hmac.init(new SecretKeySpec(peerKey, "HmacSHA256"));
        if (offset >= 0 && offset <= HANDSHAKE_LENGTH) {
            if (offset != 0) {
                sha256hmac.update(data, 0, offset);
            }
            if (offset + HANDSHAKE_DIGEST_LENGTH != HANDSHAKE_LENGTH) {
                sha256hmac.update(data, offset + HANDSHAKE_DIGEST_LENGTH, buf.length - offset - HANDSHAKE_DIGEST_LENGTH);
            }
        } else {
            sha256hmac.update(data);
        }
        return sha256hmac.doFinal();
    }

    private void stage0(ChannelHandlerContext ctx, ByteBuf bmsg) throws Exception {
        int first = bmsg.readUnsignedByte();
        if (first != 0x03) {
            throw new Exception("First byte of C0 was not 0x03, but was " + first);
        }
        RtmpClientContext client = ctx.channel().attr(RtmpServer.RTMP_CLIENT_CONTEXT).get();
        log.info("Client {} started handshake", client.getId());
        state(Stage.STAGE1);
    }
}
