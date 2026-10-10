package com.danver.messengerserver.models.util;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Direction {

    FUTURE,
    PAST;

    @JsonValue
    int toValue() {
        return this.ordinal() + 1;
    }
}
