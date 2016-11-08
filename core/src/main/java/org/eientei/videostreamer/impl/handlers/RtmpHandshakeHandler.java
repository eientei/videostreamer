package org.eientei.videostreamer.impl.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by Alexander Tumin on 2016-11-03
 */
public class RtmpHandshakeHandler extends ReplayingDecoder<RtmpHandshakeHandler.Stage> {
    public final static int HANDSHAKE_LENGTH = 1536;
    public final static int HANDSHAKE_DIGEST_LENGTH = 32;
    public final static byte[] CLIENT_KEY = {
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

    public final static byte[] SERVER_KEY = {
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


    public final static byte[] CLIENT_KEY_TEXT = Arrays.copyOf(CLIENT_KEY, 30);

    public final static byte[] SERVER_KEY_TEXT = Arrays.copyOf(SERVER_KEY, 36);

    public final static byte[] SERVER_VERSION = {
            (byte) 0x0D, (byte) 0x0E, (byte) 0x0A, (byte) 0x0D
    };
    private final Runnable initializer;

    private ByteBuf buf;

    public enum Stage {
        STAGE0,
        STAGE1,
        STAGE2
    }

    public RtmpHandshakeHandler(Runnable initializer) {
        this.initializer = initializer;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case STAGE0:
                stage0(ctx, in);
                checkpoint(Stage.STAGE1);
                break;
            case STAGE1:
                stage1(ctx, in);
                checkpoint(Stage.STAGE2);
                break;
            case STAGE2:
                stage2(ctx, in);
                checkpoint();
                if (initializer != null) {
                    initializer.run();
                }
                ctx.pipeline().remove(this);
                break;
        }
    }

    private void stage2(ChannelHandlerContext ctx, ByteBuf in) {
        in.skipBytes(HANDSHAKE_LENGTH);
    }

    private void stage1(ChannelHandlerContext ctx, ByteBuf in) throws InvalidKeyException, NoSuchAlgorithmException {
        in.readBytes(buf, 0, buf.capacity());
        buf.writerIndex(buf.capacity());
        int offset = findDigest(buf, 772, CLIENT_KEY_TEXT);
        if (offset == -1) {
            offset = findDigest(buf, 8, CLIENT_KEY_TEXT);
        }

        if (offset != -1) {
            ByteBuf slice = buf.slice(offset, HANDSHAKE_DIGEST_LENGTH);
            byte[] digest = makeDigest(slice, -1, SERVER_KEY);
            newHandshake(ctx, buf, digest);
        } else {
            oldHandshake(ctx, buf);
        }
    }

    private void stage0(ChannelHandlerContext ctx, ByteBuf in) {
        if (in.readByte() != 0x03) {
            ctx.close();
            return;
        }
        buf = ctx.alloc().heapBuffer(HANDSHAKE_LENGTH);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        state(Stage.STAGE0);
    }

    @Override
    protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
        if (buf != null) {
            buf.release();
            buf = null;
        }
        super.handlerRemoved0(ctx);
    }

    private static int findDigest(ByteBuf data, int base, byte[] peerKey) throws InvalidKeyException, NoSuchAlgorithmException {
        int offset = findOffset(data, base);
        byte[] digest = makeDigest(data, offset, peerKey);
        byte[] reference = new byte[HANDSHAKE_DIGEST_LENGTH];
        data.getBytes(offset, reference, 0, HANDSHAKE_DIGEST_LENGTH);
        if (Arrays.equals(digest, reference)) {
            return offset;
        }
        return -1;
    }

    public static int findOffset(ByteBuf data, int base) {
        int offset = 0;
        for (int n = 0; n < 4; n++) {
            offset += data.getUnsignedByte(base + n);
        }

        return (offset % 728) + base + 4;
    }

    public static byte[] makeDigest(ByteBuf slice, int offset, byte[] peerKey) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256hmac = null;
        sha256hmac = Mac.getInstance("HmacSHA256");
        sha256hmac.init(new SecretKeySpec(peerKey, "HmacSHA256"));

        if (offset >= 0 && offset <= HANDSHAKE_LENGTH) {
            if (offset != 0) {
                sha256hmac.update(slice.array(), slice.arrayOffset(), offset);
            }
            if (offset + HANDSHAKE_DIGEST_LENGTH != HANDSHAKE_LENGTH) {
                sha256hmac.update(slice.array(), slice.arrayOffset() + offset + HANDSHAKE_DIGEST_LENGTH, slice.capacity() - offset - HANDSHAKE_DIGEST_LENGTH);
            }
        } else {
            byte[] dat = new byte[HANDSHAKE_DIGEST_LENGTH];
            slice.readBytes(dat);
            sha256hmac.update(dat);
        }
        return sha256hmac.doFinal();
    }

    private static void commonHandshake(ChannelHandlerContext ctx, ByteBuf buf) throws NoSuchAlgorithmException, InvalidKeyException {
        buf.setInt(0, 0);
        buf.setBytes(4, SERVER_VERSION);

        randomize(buf, 8);

        writeDigest(buf, SERVER_KEY_TEXT);

        ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(new byte[]{0x03})).syncUninterruptibly();
        buf.retain();
        ctx.channel().writeAndFlush(buf).syncUninterruptibly();
    }

    private static void oldHandshake(ChannelHandlerContext ctx, ByteBuf buf) throws InvalidKeyException, NoSuchAlgorithmException {
        ByteBuf echo = buf.copy();

        commonHandshake(ctx, buf);


        ctx.channel().writeAndFlush(echo).syncUninterruptibly();
    }

    private static void newHandshake(ChannelHandlerContext ctx, ByteBuf buf, byte[] digest) throws InvalidKeyException, NoSuchAlgorithmException {
        commonHandshake(ctx, buf);

        randomize(buf, 8);
        digest = makeDigest(buf, HANDSHAKE_LENGTH - HANDSHAKE_DIGEST_LENGTH, digest);
        buf.setBytes(HANDSHAKE_LENGTH - HANDSHAKE_DIGEST_LENGTH, digest);
        buf.retain();
        ctx.channel().writeAndFlush(buf).syncUninterruptibly();
    }

    private static void randomize(ByteBuf data, int start) {
        Random random = new Random();
        for (; start < data.capacity(); start++) {
            data.setByte(start, (byte) random.nextInt(8));
        }
    }

    private static void writeDigest(ByteBuf data, byte[] key) throws InvalidKeyException, NoSuchAlgorithmException {
        int offset = 0;
        for (int n = 8; n < 12; n++) {
            offset += data.getUnsignedByte(n);
        }
        offset = (offset % 728) + 12;
        byte[] digest = makeDigest(data, offset, key);
        data.setBytes(offset, digest);
    }
}
