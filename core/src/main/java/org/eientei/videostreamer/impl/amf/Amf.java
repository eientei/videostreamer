package org.eientei.videostreamer.impl.amf;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class Amf {

    public enum Type {
        NUMBER(0),
        BOOL(1),
        STRING(2),
        OBJECT(3),
        NULL(5),
        UNDEFINED(6),
        MAP(8),
        END(9),
        ARRAY(10);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Type dispatch(int value) {
            for (Type type : Type.values()) {
                if (type.getValue() == value) {
                    return type;
                }
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static void serialize(ByteBuf data, Object... objects) {
        for (Object object : objects) {
            if (object instanceof Number) {
                writeNumber(data, (Number)object);
            } else if (object instanceof Boolean) {
                writeBool(data, (Boolean)object);
            } else if (object instanceof String) {
                writeString(data, (String)object);
            } else if (object instanceof AmfMap) {
                writeObject(data, (AmfMap)object);
            } else if (object instanceof Map) {
                writeMap(data, (Map)object);
            } else if (object instanceof List) {
                writeArray(data, (List)object);
            } else if (object == null) {
                writeNull(data);
            }
        }
    }

    public static void writeMap(ByteBuf data, Map<String, Object> object) {
        data.writeByte(Type.MAP.getValue());
        data.writeInt(0);
        writeAssoc(data, object);
    }

    public static void writeNull(ByteBuf data) {
        data.writeByte(Type.NULL.getValue());
    }

    public static void writeArray(ByteBuf data, List object) {
        data.writeByte(Type.ARRAY.getValue());
        data.writeInt(object.size());
        for (Object v : object) {
            serialize(data, v);
        }
    }

    public static void writeObject(ByteBuf data, Map<String, Object> object) {
        data.writeByte(Type.OBJECT.getValue());
        writeAssoc(data, object);
    }

    private static void writeAssoc(ByteBuf data, Map<String, Object> object) {
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            data.writeShort(entry.getKey().length());
            data.writeCharSequence(entry.getKey(), CharsetUtil.UTF_8);
            serialize(data, entry.getValue());
        }
        data.writeShort(0);
        data.writeByte(Type.END.getValue());
    }

    public static void writeString(ByteBuf data, String object) {
        data.writeByte(Type.STRING.getValue());
        data.writeShort(object.length());
        data.writeCharSequence(object, CharsetUtil.UTF_8);
    }

    public static void writeBool(ByteBuf data, Boolean object) {
        data.writeByte(Type.BOOL.getValue());
        data.writeByte(object ? 1 : 0);
    }

    public static void writeNumber(ByteBuf data, Number object) {
        data.writeByte(Type.NUMBER.getValue());
        data.writeDouble(object.doubleValue());
    }

    public static AmfMap makeObject(Object... pairs) {
        AmfMap map = new AmfMap();
        for (int i = 0; i < pairs.length; i+=2) {
            map.put(pairs[i].toString(), pairs[i+1]);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(ByteBuf data) {
        int typebyte = data.readUnsignedByte();
        switch (Type.dispatch(typebyte)) {
            case NUMBER:
                return (T) readNumber(data);
            case BOOL:
                return (T) readBool(data);
            case STRING:
                return (T) readString(data);
            case OBJECT:
                return (T) readObject(data);
            case MAP:
                return (T) readMap(data);
            case ARRAY:
                return (T) readArray(data);
        }
        return null;
    }

    public static AmfList deserializeAll(ByteBuf data) {
        AmfList list = new AmfList();
        while (data.isReadable()) {
            list.add(deserialize(data));
        }
        return list;
    }

    public static AmfList readArray(ByteBuf data) {
        AmfList result = new AmfList();
        long siz = data.readUnsignedInt();
        for (int i = 0; i < siz; i++) {
            result.add(deserialize(data));
        }
        return result;
    }

    public static Map<String, Object> readMap(ByteBuf data) {
        data.skipBytes(4);
        return readAssoc(data);
    }

    private static Map<String, Object> readAssoc(ByteBuf data) {
        Map<String, Object> map = new HashMap<>();
        while(data.isReadable()) {
            String key = readString(data);
            Object value = deserialize(data);
            if (key.length() == 0 && value == null) {
                break;
            }
            map.put(key, value);
        }
        return map;
    }

    public static AmfMap readObject(ByteBuf data) {
        return new AmfMap(readAssoc(data));
    }

    public static String readString(ByteBuf data) {
        int len = data.readUnsignedShort();
        return String.valueOf(data.readCharSequence(len, CharsetUtil.UTF_8));
    }

    public static Boolean readBool(ByteBuf data) {
        return data.readByte() != 0;
    }

    public static Double readNumber(ByteBuf data) {
        return data.readDouble();
    }
}
