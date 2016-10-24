package org.eientei.videostreamer.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.SocketChannel;
import org.eientei.videostreamer.amf.Amf;
import org.eientei.videostreamer.amf.AmfListWrapper;
import org.eientei.videostreamer.amf.AmfObjectWrapper;
import org.eientei.videostreamer.rtmp.message.*;
import org.eientei.videostreamer.util.PooledAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-10-19
 */
public class RtmpContext implements RtmpSubscriber {
    public final PooledAllocator ALLOC = new PooledAllocator();
    private final Logger log = LoggerFactory.getLogger(RtmpContext.class);

    private final ByteBuf handshakeBuf = ALLOC.alloc(RtmpHandshake.HANDSHAKE_LENGTH);
    private final SocketChannel channel;
    private final String id;
    private final RtmpServer server;
    private final Map<Integer, RtmpAssembler> assembly = new HashMap<>();
    private final Map<Integer, RtmpDisassembler> disassembly = new HashMap<>();
    private int chunkin = 128;
    private int chunkout = 128;
    private RtmpStream stream;
    private RtmpRole role;
    private int lastChunkin;
    private int readcount;
    private int ackwindow = 5000000;

    public RtmpContext(SocketChannel channel, RtmpServer server) {
        this.channel = channel;
        this.id = "<" + channel.id().asShortText() + ">";
        this.server = server;
    }

    public String getId() {
        return id;
    }

    @Override
    public synchronized void acceptVideo(ByteBuf readonly, int timestamp) {
        channel.writeAndFlush(new RtmpVideoMessage(6, 1, timestamp, readonly)).syncUninterruptibly();
    }

    @Override
    public synchronized void acceptAudio(ByteBuf readonly, int timestamp) {
        channel.writeAndFlush(new RtmpAudioMessage(4, 1, timestamp, readonly)).syncUninterruptibly();
    }

    @Override
    public synchronized void begin(AmfObjectWrapper metadata, ByteBuf videoro, ByteBuf audioro) {
        channel.writeAndFlush(new RtmpUserMessage(2, 0, 0, this,
                RtmpUserMessage.Event.STREAM_BEGIN,
                1,
                0
        )).syncUninterruptibly();
        channel.writeAndFlush(new RtmpCmdMessage(5, 1, 0, this,
                "onStatus",
                0.0,
                null,
                Amf.makeObject(
                        "level", "status",
                        "code", "NetStream.Play.Start",
                        "description", "Start live."
                )
        )).syncUninterruptibly();
        channel.writeAndFlush(new RtmpMetaMessage(5, 1, 0, this,
                "|RtmpSampleAccess",
                true,
                true
        )).syncUninterruptibly();
        channel.writeAndFlush(new RtmpCmdMessage(5, 1, 0, this,
                "onStatus",
                0.0,
                null,
                Amf.makeObject(
                        "level", "status",
                        "code", "NetStream.Play.PublishNotify",
                        "description", "Start publishing."
                )
        )).syncUninterruptibly();


        channel.writeAndFlush(new RtmpMetaMessage(5, 1, 0, this, "onMetaData", metadata)).syncUninterruptibly();
        videoro.retain();
        channel.writeAndFlush(new RtmpVideoMessage(6, 1, 0, videoro)).syncUninterruptibly().syncUninterruptibly();
        audioro.retain();
        channel.writeAndFlush(new RtmpAudioMessage(4, 1, 0, audioro)).syncUninterruptibly().syncUninterruptibly();
    }

    @Override
    public synchronized void finish() {
        channel.writeAndFlush(new RtmpUserMessage(2, 0, 0, this, RtmpUserMessage.Event.STREAM_EOF, 1, 0)).syncUninterruptibly();
        channel.writeAndFlush(new RtmpCmdMessage(5, 1, 0, this,
                "onStatus",
                0.0,
                null,
                Amf.makeObject(
                        "level", "status",
                        "code", "NetStream.Play.Stop",
                        "description", "Stop live."
                )
        )).syncUninterruptibly();
        channel.writeAndFlush(new RtmpCmdMessage(5, 1, 0, this,
                "onStatus",
                0.0,
                null,
                Amf.makeObject(
                        "level", "status",
                        "code", "NetStream.Play.UnpublishNotify",
                        "description", "Stop publishing."
                )
        )).syncUninterruptibly();
    }

    public void close() {
        if (stream != null && role != null) {
            switch (role) {
                case PUBLISHER:
                    stream.unpublish(this);
                    break;
                case SUBSCRIBER:
                    stream.unsubscribe(this);
                    break;
            }
        }
        if (channel.isOpen()) {
            channel.close();
        }
        if (handshakeBuf.refCnt() > 0) {
            handshakeBuf.release();
        }
        for (RtmpAssembler assembler : assembly.values()) {
            assembler.relase();
        }
        ALLOC.releasePool();
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public RtmpAssembler getAssembler(int chunk) {
        if (!assembly.containsKey(chunk)) {
            assembly.put(chunk, new RtmpAssembler(chunk, this));
        }
        return assembly.get(chunk);
    }

    public RtmpDisassembler getDisassembler(int chunk) {
        if (!disassembly.containsKey(chunk)) {
            disassembly.put(chunk, new RtmpDisassembler(chunk, this));
        }
        return disassembly.get(chunk);
    }

    public ByteBuf getHandshakeBuf() {
        return handshakeBuf;
    }

    public int getChunkin() {
        return chunkin;
    }

    public void setChunkin(int chunkin) {
        this.chunkin = chunkin;
    }

    public int getChunkout() {
        return chunkout;
    }

    public void setChunkout(int chunkout) {
        this.chunkout = chunkout;
    }

    public RtmpRole getRole() {
        return role;
    }

    public void setRole(RtmpRole role) {
        this.role = role;
    }

    public int getLastChunkin() {
        return lastChunkin;
    }

    public void setLastChunkin(int lastChunkin) {
        this.lastChunkin = lastChunkin;
    }

    public RtmpStream getStream() {
        return stream;
    }

    public void setStream(RtmpStream stream) {
        this.stream = stream;
    }

    public int getReadcount() {
        return readcount;
    }

    public void setReadcount(int readcount) {
        this.readcount = readcount;
    }

    public int getAckwindow() {
        return ackwindow;
    }

    public void setAckwindow(int ackwindow) {
        this.ackwindow = ackwindow;
    }

    @Override
    public String toString() {
        return id;
    }

    public void process(RtmpSetChunkMessage rtmpSetChunkMessage) {
        chunkin = rtmpSetChunkMessage.getChunksize();
    }

    public void process(RtmpAckMessage rtmpAckMessage) {
        // ignore
    }

    public void process(RtmpUserMessage rtmpUserMessage) {
        // ignore
    }

    public void process(RtmpWinackMessage rtmpWinackMessage) {
        ackwindow = rtmpWinackMessage.getWinsize();
    }

    public void process(RtmpSetPeerBandMessage rtmpSetPeerBandMessage) {
        // ignore
    }

    public void process(RtmpAudioMessage rtmpAudioMessage) {
        stream.broadcastAudio(rtmpAudioMessage);
    }

    public void process(RtmpVideoMessage rtmpVideoMessage) {
        stream.broadcastVideo(rtmpVideoMessage);
    }

    @SuppressWarnings("unchecked")
    public void process(RtmpCmdMessage rtmpCmdMessage) {
        AmfListWrapper amf = Amf.deserializeAll(rtmpCmdMessage.getData());
        String cmd = amf.get(0);
        log.info("{}: issued {}", this, cmd);
        switch (cmd) {
            case "connect":
                channel.writeAndFlush(new RtmpWinackMessage(2, 0, 0, this, 5000000)).syncUninterruptibly();
                channel.writeAndFlush(new RtmpSetPeerBandMessage(2, 0, 0, this, 5000000, (byte) 2)).syncUninterruptibly();
                channel.writeAndFlush(new RtmpSetChunkMessage(2, 0, 0, this, 2048)).syncUninterruptibly();
                chunkout = 2048;
                channel.writeAndFlush(new RtmpCmdMessage(3, 0, 0, this,
                        "_result",
                        amf.get(1),
                        Amf.makeObject(
                                "fmsVer", "FMS/3,0,1,123",
                                "capabilities", 31.0
                        ),
                        Amf.makeObject(
                                "level", "status",
                                "code", "NetConnection.Connect.Success",
                                "description", "Connection succeeded.",
                                "objectEncoding", 3.0
                        )
                )).syncUninterruptibly();
                break;
            case "createStream":
                channel.writeAndFlush(new RtmpCmdMessage(3, 0, 0, this,
                        "_result",
                        amf.get(1),
                        null,
                        1.0
                )).syncUninterruptibly();
                break;
            case "publish":
                stream = server.acquireStream((String) amf.get(3));
                if (stream == null) {
                    channel.close();
                    return;
                }
                channel.writeAndFlush(new RtmpCmdMessage(3, 0, 0, this,
                        "onStatus",
                        0.0,
                        null,
                        Amf.makeObject(
                                "level", "status",
                                "code", "NetStream.Publish.Start",
                                "description", "Start publising."
                        )
                )).syncUninterruptibly();
                stream.publish(this);
                role = RtmpRole.PUBLISHER;
                break;
            case "play":
                stream = server.acquireStream((String) amf.get(3));
                if (stream == null) {
                    channel.close();
                    return;
                }
                stream.subscribe(this);
                role = RtmpRole.SUBSCRIBER;
                break;
        }
    }

    public void process(RtmpMetaMessage rtmpMetaMessage) {
        stream.broadcastMeta(rtmpMetaMessage);
    }

    public void updateRead(int cnt) {
        readcount += cnt;
        if (readcount >= ackwindow) {
            channel.writeAndFlush(new RtmpAckMessage(2, 0, 0, this, readcount)).syncUninterruptibly();
            readcount = 0;
        }
    }

}