package com.danver.messengerserver.models;

import com.danver.messengerserver.models.util.Direction;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class ChatPagingDTO {
    private Long userId;
    @JsonAlias("threshold")
    private Instant time;
    @JsonAlias("chatIdThreshold")
    private Long chatId;
    @Builder.Default
    private Integer count = 50;
    @Builder.Default
    private Direction direction = Direction.PAST;
}

