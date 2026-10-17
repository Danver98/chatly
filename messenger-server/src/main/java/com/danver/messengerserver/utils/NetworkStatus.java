package com.danver.messengerserver.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NetworkStatus {
    OFFLINE(0),
    ONLINE(1);

    private final int code;

    NetworkStatus(int code) {
        this.code = code;
    }

    @JsonValue
    public int toValue() {
        return code;
    }

    @JsonCreator
    public static NetworkStatus fromValue(int value) {
        for (NetworkStatus s : values()) {
            if (s.code == value) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown NetworkStatus code: " + value);
    }
}