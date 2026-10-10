package com.danver.messengerserver.models;

import com.danver.messengerserver.models.util.Direction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;


@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequestDTO {

    private Long chatId;
    private Long userId;
    private Instant time;
    private String messageId;
    private Direction direction;
    private Integer count;

    // For better times
/*    private final Map<String, Object> requestData = new HashMap<>(); // Make it Concurrent?

    public void set(String key, Object value) {
        requestData.put(key, value);
    }

    public Object get(String key) {
        return requestData.get(key);
    }

    public void clear() {
        this.requestData.clear();
    }*/
}
