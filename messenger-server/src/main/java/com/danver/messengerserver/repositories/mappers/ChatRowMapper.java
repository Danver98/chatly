package com.danver.messengerserver.repositories.mappers;

import com.danver.messengerserver.models.*;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.List;

public class ChatRowMapper implements RowMapper<Chat> {
    @Override
    public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
        Chat chat = new Chat();
        chat.setId(rs.getLong("id"));
        boolean isPrivate = rs.getBoolean("private");
        if (isPrivate) {
            chat.setName(rs.getString("user_names"));
        } else {
            chat.setName(rs.getString("name"));
        }
        chat.setAvatarUrl(rs.getString("avatarUrl"));
        OffsetDateTime timestamp = rs.getObject("lastChanged", OffsetDateTime.class);
        chat.setLastChanged(timestamp != null ? timestamp.toInstant() : Instant.now());

        chat.setPrivate(isPrivate);
        chat.setDraft(rs.getBoolean("draft"));
        Array array = rs.getArray("participants");
        if (array != null) {
            Long [] lst = (Long[]) array.getArray();
            chat.setParticipants(List.of(lst));
        }
        OffsetDateTime msgCreated = rs.getObject("message.lastChanged", OffsetDateTime.class);
        Message lastReadMsg = null;
        if (rs.getString("lastReadMsg") != null) {
            lastReadMsg = Message.builder().id(rs.getString("lastReadMsg"))
                    .time(msgCreated.toInstant())
                    .build();
        }
        chat.setLastReadMsg(lastReadMsg);
        chat.setUnreadMsgCount(rs.getInt("unreadMsgCount"));
        chat.setAuthorId(rs.getLong("authorId"));
        chat.setCanAddUsers(rs.getBoolean("canAddUsers"));
        return chat;
    }
}
