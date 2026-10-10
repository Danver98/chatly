package com.danver.messengerserver.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
public class MessageData {
    MessageDataType type;
    @JsonProperty("data")
    @JsonAlias("data")
    Object value;
}
