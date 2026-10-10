package com.danver.messengerserver.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {
    long chatId;
    long userId;
    String messageId;
    long[] users;
    String joinToken;
}
