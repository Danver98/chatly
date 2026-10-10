package com.danver.messengerserver.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

public class Chat {
    @Setter
    @Getter
    private long id;
    @Setter
    @Getter
    private String name;
    @Setter
    @Getter
    @JsonProperty("avatar")
    @JsonAlias("avatar")
    private String avatarUrl;
    @Setter
    @JsonProperty("time")
    @JsonAlias("time")
    private Instant lastChanged;

    @Getter
    @Setter
    @JsonAlias("private")
    @JsonProperty("private")
    private boolean isPrivate;
    @Getter
    private List<Long> participants;
    @Getter
    private List<Message> messages;
    @Setter
    @Getter
    private Message lastMessage;
    @Getter
    @Setter
    private boolean draft;
    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int unreadMsgCount;
    @Getter
    @Setter
    private Message lastReadMsg;

    @Getter
    @Setter
    private boolean canAddUsers;

    @Getter
    @Setter
    private long authorId;

    public Chat() {

    }

    public Chat(long id, String name, String avatarUrl, Instant lastChanged, boolean isPrivate, boolean draft) {
        this.id = id;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.lastChanged = lastChanged;
        this.isPrivate = isPrivate;
        this.draft = draft;
    }

    public void setParticipants(List<Long> participants) {

        this.participants = participants;
        if (this.participants.size() > 2) {
            this.setPrivate(false);
        }
    }
}
