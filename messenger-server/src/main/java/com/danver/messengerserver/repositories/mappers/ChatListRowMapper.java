package com.danver.messengerserver.repositories.mappers;

import com.danver.messengerserver.models.*;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

public class ChatListRowMapper implements RowMapper<Chat> {
    @Override
    public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
        Chat chat = new Chat();
        chat.setId(rs.getLong("id"));
        chat.setName(rs.getString("name"));
        chat.setAvatarUrl(rs.getString("avatarUrl"));
        OffsetDateTime timestamp = rs.getObject("lastChanged", OffsetDateTime.class);
        chat.setLastChanged(timestamp != null ? timestamp.toInstant() : Instant.now());

        chat.setPrivate(rs.getBoolean("private"));
        chat.setDraft(rs.getBoolean("draft"));

        Message lastMessage = Message.builder()
                .id(rs.getString("lastMsg.id"))
                .chatId(rs.getLong("lastMsg.chatId"))
                .build();

        lastMessage.setAuthor(
                User.builder()
                        .id(rs.getLong("lastMsg.authorId"))
                        .name(rs.getString("lastMsg.authorName"))
                        .surname(rs.getString("lastMsg.authorSurname"))
                        .avatarUrl(rs.getString("lastMsg.authorAvatarUrl"))
                        .build()
        );
        OffsetDateTime lastMessageTimestamp = rs.getObject("lastMsg.lastChanged", OffsetDateTime.class);
        lastMessage.setTime(lastMessageTimestamp == null ? null : lastMessageTimestamp.toInstant());
        // What's if null?
        Short valueType = rs.getObject("lastMsg.valueType", Short.class);
        Short type = rs.getObject("lastMsg.type", Short.class);
        Object value = rs.getObject("lastMsg.value");
        lastMessage.setData(new MessageData(valueType != null ? MessageDataType.get(valueType.byteValue()) : null, value));
        lastMessage.setType(type != null ? Message.MessageType.get(type.byteValue()) : null);

        chat.setLastMessage(lastMessage);
        chat.setUnreadMsgCount(rs.getInt("unreadMsgCount"));
        return chat;
    }
}

