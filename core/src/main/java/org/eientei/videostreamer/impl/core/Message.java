package org.eientei.videostreamer.impl.core;

import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import org.eientei.videostreamer.impl.amf.Amf;
import org.eientei.videostreamer.impl.amf.AmfList;

import java.util.Objects;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class Message extends AbstractReferenceCounted {
    public enum Type {
        SET_CHUNK_SIZE(1),
        ACK(3),
        USER(4),
        WINACK(5),
        SET_PEER_BAND(6),
        AUDIO(8),
        VIDEO(9),
        AMF3_CMD_ALT(11),
        AMF3_META(15),
        AMF3_CMD(17),
        AMF0_META(18),
        AMF0_CMD(20);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private final Header header;
    private final ByteBuf data;
    private AmfList list;

    public Message(Header header, ByteBuf data) {
        this.header = new Header(header, data);
        data.retain();
        this.data = data;
    }

    public Header getHeader() {
        return header;
    }

    public ByteBuf getData() {
        return data.slice();
    }

    public ByteBuf getData(int begin, int length) {
        return data.slice(begin, length);
    }

    @Override
    protected void deallocate() {
        data.release();
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }

    public AmfList asAmf() {
        if (list == null) {
            ByteBuf dat = data.slice();
            switch (header.getType()) {
                case AMF3_CMD_ALT:
                case AMF3_META:
                case AMF3_CMD:
                    dat.skipBytes(1);
                    break;
            }
            list = Amf.deserializeAll(dat);
        }
        return list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(header, message.header);
    }
}
