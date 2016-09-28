package org.eientei.videostreamer.amf;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexander Tumin on 2016-09-26
 */
public class Amf {
    private static final AmfSerial<Double> NUMBER_SERIAL = new AmfSerial<Double>() {
        @Override
        public Double deserialize(ByteBuf data) {
            return data.readDouble();
        }

        @Override
        public void serialize(ByteBuf data, Object obj) {
            data.writeDouble((Double)obj);
        }
    };

    private static final AmfSerial<Boolean> BOOL_SERIAL = new AmfSerial<Boolean>() {
        @Override
        public Boolean deserialize(ByteBuf data) {
            return data.readByte() != 0;
        }

        @Override
        public void serialize(ByteBuf data, Object obj) {
            if ((Boolean)obj) {
                data.writeByte(1);
            } else {
                data.writeByte(0);
            }
        }
    };

    private static final AmfSerial<String> STRING_SERIAL = new AmfSerial<String>() {
        @Override
        public String deserialize(ByteBuf data) {
            int len = data.readUnsignedShort();
            return String.valueOf(data.readCharSequence(len, CharsetUtil.UTF_8));
        }

        @Override
        public void serialize(ByteBuf data, Object obj) {
            data.writeShort(((String)obj).length());
            data.writeCharSequence(((String)obj), CharsetUtil.UTF_8);
        }
    };

    private static final AmfSerial<Map<String, Object>> OBJECT_SERIAL = new AmfSerial<Map<String, Object>>() {
        @Override
        public Map<String, Object> deserialize(ByteBuf data) {
            Map<String, Object> map = new HashMap<>();
            while(true) {
                String key = STRING_SERIAL.deserialize(data);
                Object value = Amf.deserialize(data);
                if (key.length() == 0 && value == Type.END) {
                    break;
                }
                map.put(key, value);
            }
            return map;
        }

        @Override
        public void serialize(ByteBuf data, Object obj) {
            for (Map.Entry entry : ((Map<?,?>)obj).entrySet()) {
                STRING_SERIAL.serialize(data, entry.getKey());
                Amf.serialize(data, entry.getValue());
            }
            data.writeMedium(Type.END.getValue());
        }
    };

    private static final AmfSerial<Void> NULL_SERIAL = new AmfSerial<Void>() {
        @Override
        public Void deserialize(ByteBuf data) {
            return null;
        }

        @Override
        public void serialize(ByteBuf data, Object obj) {

        }
    };

    private static final AmfSerial<Map<String, Object>> MAP_SERIAL = new AmfSerial<Map<String, Object>>() {
        @Override
        public Map<String, Object> deserialize(ByteBuf data) {
            data.skipBytes(4);
            return OBJECT_SERIAL.deserialize(data);
        }

        @Override
        public void serialize(ByteBuf data, Object obj) {
            OBJECT_SERIAL.serialize(data, obj);
        }
    };

    private static final AmfSerial<Type> END_SERIAL = new AmfSerial<Type>() {
        @Override
        public Type deserialize(ByteBuf data) {
            return Type.END;
        }

        @Override
        public void serialize(ByteBuf data, Object obj) {

        }
    };

    private static final AmfSerial<List<Object>> ARRAY_SERIAL = new AmfSerial<List<Object>>() {
        @Override
        public List<Object> deserialize(ByteBuf data) {
            List<Object> result = new ArrayList<>();
            long siz = data.readUnsignedInt();
            for (int i = 0; i < siz; i++) {
                result.add(Amf.deserialize(data));
            }
            return result;
        }

        @Override
        public void serialize(ByteBuf data, Object obj) {
            data.writeInt(((List)obj).size());
            for (Object v : (List)obj) {
                Amf.serialize(data, v);
            }
        }
    };


    public enum Type {
        NUMBER(0x00, NUMBER_SERIAL),
        BOOL(0x01, BOOL_SERIAL),
        STRING(0x02, STRING_SERIAL),
        OBJECT(0x03, OBJECT_SERIAL),
        UNKNOWN4(0x04, NULL_SERIAL),
        NULL(0x05, NULL_SERIAL),
        UNDEFINED(0x06, NULL_SERIAL),
        UNKNOWN7(0x07, NULL_SERIAL),
        MAP(0x08, MAP_SERIAL),
        END(0x09, END_SERIAL),
        ARRAY(0x0a, ARRAY_SERIAL);

        private final int value;
        private final AmfSerial serial;

        Type(int value, AmfSerial serial) {
            this.value = value;
            this.serial = serial;
        }

        public int getValue() {
            return value;
        }

        public AmfSerial getSerial() {
            return serial;
        }

        public static Type parseValue(int value) {
            if (value >= 0 && value < values().length) {
                return values()[value];
            }
            throw new IllegalArgumentException("Illegal Amf.Type value: " + value);
        }//

        public static Type parseObject(Object object) {

            if (object instanceof Number) {
                return NUMBER;
            } else if (object instanceof Boolean) {
                return BOOL;
            } else if (object instanceof String) {
                return STRING;
            } else if (object instanceof AmfObjectWrapper) {
                return OBJECT;
            } else if (object instanceof Map) {
                return MAP;
            } else if (object instanceof List) {
                return ARRAY;
            } else if (object != null) {
                return OBJECT;
            }
            return NULL;
        }
    }

    public static <K,V> Map<K,V> makeObject(Map<K,V> map) {
        return new AmfObjectWrapper<K,V>(map);
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(ByteBuf buf) {
        int type = buf.readUnsignedByte();
        return (T) Type.parseValue(type).getSerial().deserialize(buf);
    }

    public static void serialize(ByteBuf buf, Object obj) {
        Type type = Type.parseObject(obj);
        buf.writeByte(type.getValue());
        type.getSerial().serialize(buf, obj);
    }
}
