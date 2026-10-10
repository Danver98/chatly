package com.danver.messengerserver.repositories.mappers;

import com.danver.messengerserver.models.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        return User.builder().id(rs.getLong("id"))
                .name(rs.getString("name"))
                .surname(rs.getString("surname"))
                .email(rs.getString("email"))
                .password(rs.getString("passwordhash"))
                .avatarUrl(rs.getString("avatarUrl"))
                .build();
    }
}
