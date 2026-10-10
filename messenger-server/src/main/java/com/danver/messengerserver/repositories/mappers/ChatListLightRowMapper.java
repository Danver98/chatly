package com.danver.messengerserver.repositories.mappers;

import com.danver.messengerserver.models.*;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class ChatListLightRowMapper implements RowMapper<Chat> {
    @Override
    public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
        Chat chat = new Chat();
        chat.setId(rs.getLong("id"));
        chat.setPrivate(rs.getBoolean("private"));
        chat.setDraft(rs.getBoolean("draft"));
        return chat;
    }
}

