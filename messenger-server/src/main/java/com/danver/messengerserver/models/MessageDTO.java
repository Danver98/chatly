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
public class MessageDTO {

    private Message message;
    private boolean chatIsPrivate;
    private String chatName;
    private Chat chat;
}
