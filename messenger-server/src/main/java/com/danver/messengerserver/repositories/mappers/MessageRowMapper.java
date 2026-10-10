package com.danver.messengerserver.repositories.mappers;

import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.MessageData;
import com.danver.messengerserver.models.MessageDataType;
import com.danver.messengerserver.models.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class MessageRowMapper implements RowMapper<Message> {
    @Override
    public Message mapRow(ResultSet rs, int rowNum) throws SQLException {
        Message message = new Message();
        message.setId(rs.getString("id"));
        message.setChatId(rs.getLong("chatId"));
        message.setAuthor(
                User.builder()
                        .id(rs.getLong("authorId"))
                        .name(rs.getString("name"))
                        .surname(rs.getString("surname"))
                        .avatarUrl(rs.getString("avatarUrl"))
                        .build()
        );
        OffsetDateTime timestamp = rs.getObject("lastChanged", OffsetDateTime.class);
        message.setTime(timestamp == null ? null : timestamp.toInstant());
        // What's if null?
        Short valueType = rs.getObject("value_type", Short.class);
        Short type = rs.getObject("type", Short.class);
        Object value = rs.getObject("value");
        message.setData(new MessageData(valueType != null ? MessageDataType.get(valueType.byteValue()) : null, value));
        message.setType(type != null ? Message.MessageType.get(type.byteValue()) : null);
        message.setRead(rs.getBoolean("read"));
        message.setReplyTo(rs.getString("replyTo"));
        if (message.getReplyTo() == null) return message;
        // Filling repliedMessage if present
        value = rs.getObject("replyValue");
        valueType = rs.getObject("replyValueType", Short.class);
        type = rs.getObject("replyType", Short.class);
        Message replyMessage = Message.builder()
                .id(message.getReplyTo())
                .data(new MessageData(valueType != null ? MessageDataType.get(valueType.byteValue()) : null, value))
                .type((type != null ? Message.MessageType.get(type.byteValue()) : null))
                .author(
                        User.builder()
                                .name(rs.getString("replyAuthorName"))
                                .surname(rs.getString("replyAuthorSurname"))
                                .build()
                )
                .build();
        message.setRepliedMessage(replyMessage);
        return message;
    }
}
