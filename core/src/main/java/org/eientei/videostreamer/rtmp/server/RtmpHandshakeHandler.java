package org.eientei.videostreamer.rtmp.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.eientei.videostreamer.rtmp.server.RtmpServer.CLIENT_CONTEXT;

/**
 * Created by Alexander Tumin on 2016-10-12
 */
public class RtmpHandshakeHandler extends ReplayingDecoder<RtmpHandshakeHandler.Stage> {
    enum Stage {
        STAGE0,
        STAGE1,
        STAGE2
    }

    public final static int HANDSHAKE_DIGEST_LENGTH = 32;
    public final static int HANDSHAKE_LENGTH = 1536;
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

    private final Logger log = LoggerFactory.getLogger(RtmpHandshakeHandler.class);
    private final ByteBuf data = Unpooled.buffer(HANDSHAKE_LENGTH, HANDSHAKE_LENGTH);

    public RtmpHandshakeHandler() {
        super(Stage.STAGE0);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case STAGE0:
                stage0(ctx, in);
                break;
            case STAGE1:
                stage1(ctx, in);
                break;
            case STAGE2:
                stage2(ctx, in);
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        RtmpClient client = ctx.channel().attr(CLIENT_CONTEXT).get();
        log.info("Client {} failed handshake", client.getId());
        data.release();
        ctx.close();
    }

    private void stage2(ChannelHandlerContext ctx, ByteBuf in) {
        in.skipBytes(HANDSHAKE_LENGTH);
        data.release();

        RtmpClient client = ctx.channel().attr(CLIENT_CONTEXT).get();
        log.info("{} connected", client.getId());
        ctx.pipeline().remove(this);
    }

    private void stage0(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        int first = in.readUnsignedByte();
        if (first != 0x03) {
            throw new Exception("First byte of C0 was not 0x03, but was " + first);
        }
        RtmpClient client = ctx.channel().attr(CLIENT_CONTEXT).get();
        log.info("{} started handshake", client.getId());
        state(Stage.STAGE1);
    }

    private void stage1(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        in.readBytes(data);

        int offset = findDigest(772, CLIENT_KEY_TEXT);
        if (offset == -1) {
            offset = findDigest(8, CLIENT_KEY_TEXT);
        }

        ByteBuf oldbuf = null;
        byte[] digest = null;
        if (offset != -1) {
            ByteBuf slice = data.slice(offset, HANDSHAKE_DIGEST_LENGTH);
            digest = makeDigest(slice, -1, SERVER_KEY);
        } else {
            oldbuf = data.copy();
        }

        data.setInt(0, 0);
        data.setBytes(4, SERVER_VERSION);

        randomize(8);

        writeDigest(SERVER_KEY_TEXT);

        ctx.writeAndFlush(Unpooled.wrappedBuffer(new byte[] { 0x03 })).sync();
        ctx.writeAndFlush(data.copy()).sync();

        if (digest != null) {
            randomize(8);
            digest = makeDigest(data, HANDSHAKE_LENGTH - HANDSHAKE_DIGEST_LENGTH, digest);
            data.setBytes(HANDSHAKE_LENGTH - HANDSHAKE_DIGEST_LENGTH, digest);
            ctx.writeAndFlush(data.copy()).sync();
        }

        if (oldbuf != null) {
            ctx.writeAndFlush(oldbuf).sync();
        }

        state(Stage.STAGE2);
    }

    private void writeDigest(byte[] key) throws Exception {
        int offset = 0;
        for (int n = 8; n < 12; n++) {
            offset += data.getUnsignedByte(n);
        }
        offset = (offset % 728) + 12;
        byte[] digest = makeDigest(data, offset, key);
        data.setBytes(offset, digest);
    }

    private byte[] makeDigest(ByteBuf slice, int offset, byte[] peerKey) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256hmac = Mac.getInstance("HmacSHA256");
        sha256hmac.init(new SecretKeySpec(peerKey, "HmacSHA256"));
        if (offset >= 0 && offset <= HANDSHAKE_LENGTH) {
            if (offset != 0) {
                sha256hmac.update(slice.array(), 0, offset);
            }
            if (offset + HANDSHAKE_DIGEST_LENGTH != HANDSHAKE_LENGTH) {
                sha256hmac.update(slice.array(), offset + HANDSHAKE_DIGEST_LENGTH, slice.capacity() - offset - HANDSHAKE_DIGEST_LENGTH);
            }
        } else {
            byte[] dat = new byte[HANDSHAKE_DIGEST_LENGTH];
            slice.readBytes(dat);
            sha256hmac.update(dat);
        }
        return sha256hmac.doFinal();
    }

    private int findDigest(int base, byte[] peerKey) throws Exception {
        int offset = 0;
        for (int n = 0; n < 4; n++) {
            offset += data.getUnsignedByte(base+n);
        }

        offset = (offset % 728) + base + 4;
        byte[] digest = makeDigest(data, offset, peerKey);
        byte[] reference = new byte[HANDSHAKE_DIGEST_LENGTH];
        data.getBytes(offset, reference, 0, HANDSHAKE_DIGEST_LENGTH);
        if (Arrays.equals(digest, reference)) {
            return offset;
        }
        return -1;
    }

    private void randomize(int i) {
        Random random = new Random();
        for (; i < data.capacity(); i++) {
            data.setByte(i,(byte) random.nextInt(8));
        }
    }
}
