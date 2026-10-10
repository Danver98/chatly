package com.danver.messengerserver.services.permission;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum ResourceType {

    ADMIN(1),
    USER(5),
    CHAT(15),
    CHAT_MESSAGE(20),

    GROUP(30);

    private final byte value;

    private static final Map<Byte, ResourceType> map = new HashMap<>();
    static {
        for (ResourceType mt: EnumSet.allOf(ResourceType.class)) {
            map.put(mt.getValue(), mt);
        }
    }

    ResourceType(int value) {
        this.value = (byte)value;
    }

    public byte getValue() {
        return value;
    }

    public static ResourceType get(int type) {
        return ResourceType.map.get((byte)type);
    }
}
