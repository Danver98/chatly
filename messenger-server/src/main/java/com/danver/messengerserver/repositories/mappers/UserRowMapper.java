package com.danver.messengerserver.repositories.mappers;

import com.danver.messengerserver.models.User;
import com.danver.messengerserver.utils.NetworkStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        OffsetDateTime timestamp = rs.getObject("lastVisited", OffsetDateTime.class);
        return User.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .surname(rs.getString("surname"))
                .email(rs.getString("email"))
                .password(rs.getString("passwordHash"))
                .avatarUrl(rs.getString("avatarUrl"))
                .status(NetworkStatus.fromValue(rs.getShort("networkStatus")))
                .lastVisited(timestamp == null ? null : timestamp.toInstant())
                .build();
    }
}