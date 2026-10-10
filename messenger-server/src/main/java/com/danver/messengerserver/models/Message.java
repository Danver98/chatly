package com.danver.messengerserver.models;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private String id;
    private long chatId;
    private User author;
    /**
     * In private chats it indicates who the message was sent to.
     * In public chats it serves as additional info for special message types
     * (e.g. which user was affected by this action)
     */
    private long receiverId;
    private Instant time;
    private MessageData data;
    private MessageType type;
    /**
     * Whether this message's been read by certain user
     */
    private boolean read;
    private String replyTo;
    private Message repliedMessage;

    public enum MessageType {
        CREATION(1),
        INVITATION(2),
        @JsonEnumDefaultValue
        CHAT(3),
        JOIN(4),
        LEAVE(5),
        EXCLUDE(6),
        REPLY(7),
        DELETE(8);

        private final byte value;
        private static final Map<Byte, MessageType> map = new HashMap<>();
        static {
            for (MessageType mt: EnumSet.allOf(MessageType.class)) {
                map.put(mt.getValue(), mt);
            }
        }

        MessageType(int value) {
            this.value = (byte)value;
        }
        @JsonValue
        byte getValue() {
            return this.value;
        }

        public static MessageType get(byte value) {
            return MessageType.map.get(value);
        }
    }

    public OffsetDateTime getTimeUTC() {
        Instant time = this.getTime();
        return time == null ? null : time.atOffset(ZoneOffset.UTC);
    }
}
