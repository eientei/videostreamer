package org.eientei.videostreamer.rtmp;

/**
 * Created by Alexander Tumin on 2016-10-13
 */
public enum RtmpMessageType {
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

    RtmpMessageType(int value) {
        this.value = value;
    }

    public static RtmpMessageType dispatch(int value) {
        for (RtmpMessageType t : RtmpMessageType.values()) {
            if (t.getValue() == value) {
                return t;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }
}
