package com.danver.messengerserver.models;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum MessageDataType {
    DEFAULT(1),
    @JsonEnumDefaultValue
    TEXT(2),
    IMAGE( 3),
    VIDEO(4),
    FILE(5),
    AUDIO(6),
    JOIN_LINK(7),
    ARRAY(8);

    private final byte value;
    private static final Map<Byte, MessageDataType> map = new HashMap<>();
    static {
        for (MessageDataType mt: EnumSet.allOf(MessageDataType.class)) {
            map.put(mt.getValue(), mt);
        }
    }

    MessageDataType(int value) {
        this.value = (byte)value;
    }

    @JsonValue
    byte getValue() {
        return this.value;
    }

    public static MessageDataType get(byte value) {
        return MessageDataType.map.get(value);
    }
}
