package com.danver.messengerserver.repositories.mappers;

import com.danver.messengerserver.models.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class is used to transfer relatively non-sensitive data
 */
public class UserDTORowMapper implements RowMapper<User> {
    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        return User.builder().id(rs.getLong("id"))
                .name(rs.getString("name"))
                .surname(rs.getString("surname"))
                .email(rs.getString("email"))
                .avatarUrl(rs.getString("avatarUrl"))
                .build();
    }
}
