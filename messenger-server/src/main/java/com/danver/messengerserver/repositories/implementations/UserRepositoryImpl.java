package com.danver.messengerserver.repositories.implementations;

import com.danver.messengerserver.models.User;
import com.danver.messengerserver.models.UserRequestDTO;
import com.danver.messengerserver.models.UserRequestFilter;
import com.danver.messengerserver.models.util.Direction;
import com.danver.messengerserver.repositories.interfaces.UserRepository;
import com.danver.messengerserver.repositories.mappers.UserDTORowMapper;
import com.danver.messengerserver.repositories.mappers.UserRowMapper;
import com.danver.messengerserver.utils.NetworkStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    //USERS TABLE FIELDS:
    //id, name, surname, email, salt, passwordHash, avatarUrl

    @Autowired
    public UserRepositoryImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public User createUser(User user) {
        String query = "INSERT INTO \"Users\" (name, surname, email, salt, \"passwordHash\", \"avatarUrl\")" +
                " VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        long id = jdbcTemplate.queryForObject(query, Long.class, user.getName(), user.getSurname(),
                user.getEmail(), user.getSalt(), user.getPassword(), user.getAvatarUrl());
        user.setId(id);
        return user;
    }

    @Override
    public User getUser(long id) {
        String query = """
                select
                    u."id",
                    u."name",
                    u."surname",
                    u."email",
                    u."avatarUrl",
                    coalesce(
                        (
                            select
                                :online::smallint
                            from
                                "RefreshTokens" rt
                            where
                                rt."user" = :userId
                                and rt."networkStatus" = :online::smallint
                            limit 1
                        ),
                        :offline::smallint
                    ) "status",
                    (
                        select
                            max(rt."statusUpdateTime")
                        from
                            "RefreshTokens" rt
                        where
                            rt."user" = :userId
                    ) "lastVisited"
                from
                    "Users" u
                where
                    u."id" = :userId
        """;
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("userId", id, Types.BIGINT);
        namedParameters.addValue("online", (short) NetworkStatus.ONLINE.toValue(), Types.SMALLINT);
        namedParameters.addValue("offline", (short) NetworkStatus.OFFLINE.toValue(),Types.SMALLINT);
        try {
            return namedParameterJdbcTemplate.queryForObject(query, namedParameters, new UserDTORowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public User getUserByEmail(String email) {
        log.info("Getting user with login '{}' from database", email);
        String query = """
            select
                u."id",
                u."name",
                u."surname",
                u."email",
                u."avatarUrl",
                u."passwordHash",
                u."salt",
                coalesce(
                    (
                        select
                            :online::smallint
                        from
                            "RefreshTokens" rt
                        where
                            rt."user" = u."id"
                            and rt."networkStatus" = :online::smallint
                        limit 1
                    ),
                    :offline::smallint
                ) "status",
                (
                    select
                        max(rt."statusUpdateTime")
                    from
                        "RefreshTokens" rt
                    where
                        rt."user" = u."id"
                ) "lastVisited"
            from
                "Users" u
            where
                u."email" = :email
    """;
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("email", email, Types.VARCHAR);
        namedParameters.addValue("online", (short) NetworkStatus.ONLINE.toValue(), Types.SMALLINT);
        namedParameters.addValue("offline", (short) NetworkStatus.OFFLINE.toValue(),Types.SMALLINT);
        try {
            return namedParameterJdbcTemplate.queryForObject(query, namedParameters, new UserRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void updateUser(User user) {
        String query = """
               UPDATE
                    "Users"
                SET
                    name = :name,
                    surname = :surname,
                    email = :email,
                    "avatarUrl" = :avatarUrl
               WHERE
                    id = :id
        """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("name", user.getName());
        params.addValue("surname", user.getSurname());
        params.addValue("email", user.getEmail());
        params.addValue("avatarUrl", user.getAvatarUrl());
        params.addValue("id", user.getId());
        namedParameterJdbcTemplate.update(query, params);
    }

    @Override
    public void deleteUser(long id) {
        String query = "DELETE FROM \"Users\" WHERE id = ?";
        jdbcTemplate.update(query, id);
    }

    @Override
    public List<User> list(UserRequestDTO dto) {
        String [] params;
        UserRequestFilter filter = dto.getFilter();
        String search = filter.getSearch();
        if (search == null || search.isEmpty()) {
            params = new String[] {null, null};
        } else {
            params = search.split(" ");
        }
        Long[] ids = null;
        if (filter.getIds() != null && filter.getIds().length > 0) {
            ids = Arrays.stream(filter.getIds())
                    .boxed()
                    .toArray(Long[]::new);
        }
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("name", params[0], Types.VARCHAR);
        namedParameters.addValue("searchSurname", params.length > 1 ? params[1] : null, Types.VARCHAR);
        namedParameters.addValue("chatId", filter.getChatId(), Types.BIGINT);
        namedParameters.addValue("exclude", filter.getExclude(), Types.BOOLEAN);
        namedParameters.addValue("count", filter.getCount() != null ? filter.getCount() : 20L, Types.BIGINT);
        namedParameters.addValue("cursorId", dto.getId(), Types.BIGINT);
        namedParameters.addValue("cursorSurname", dto.getSurname(), Types.VARCHAR);
        // When dealing with NamedParameterJdbcTemplate arrays in SQL-query, you should explicitly cast
        // these params to certain array type (e.g. ":ids::bigint[]"), if you have some of null check
        // (like ":ids is not null" )
        namedParameters.addValue("ids", ids, Types.ARRAY);
        namedParameters.addValue("online", (short) NetworkStatus.ONLINE.toValue(), Types.SMALLINT);
        namedParameters.addValue("offline", (short) NetworkStatus.OFFLINE.toValue(), Types.SMALLINT);
        namedParameters.addValue("networkStatus",
                filter.getStatus() != null ? (short) filter.getStatus().toValue() : null,
                Types.SMALLINT);
        char compareSign = dto.getDirection() == Direction.FUTURE ? '>' : '<';
        String order = dto.getDirection() == Direction.FUTURE ? "ASC" : "DESC";
        String query = String.format("""
        select
            u.id,
            u.name,
            u.surname,
            u.email,
            u."avatarUrl",
            u."passwordHash",
            coalesce(
                (
                    select
                        :online::smallint
                    from
                        "RefreshTokens" rt
                    where
                        rt."user" = u."id"
                        and rt."networkStatus" = :online::smallint
                    limit 1
                ),
                :offline::smallint
            ) "status",
            (
                select
                    max(rt."statusUpdateTime")
                from
                    "RefreshTokens" rt
                where
                    rt."user" = u."id"
            ) "lastVisited"
        from
            "Users" u
        where
            case
                when :cursorId is null
                    then true
                else
                    case
                        when :cursorSurname is null
                            then u.id %c :cursorId
                        else
                            (u.id, u.surname) %c (:cursorId, :cursorSurname)
                    end
            end
            and
            case
                when :cursorId is null and :ids::bigint[] is not null
                    then u."id" = any(:ids::bigint[])
                else
                    true
            end
            -- search
            and
            case
                -- name or surname or login
                when :name is not null
                    then
                        (u."email"  ilike '%%' || :name || '%%'
                         or u.name ilike '%%' || :name || '%%'
                         or u.surname ilike '%%' || :name || '%%'
                        )
                else
                    true
            end
            and
            case
                when :searchSurname is not null
                    then
                        (u.name ilike '%%' || :searchSurname || '%%'
                         or u.surname ilike '%%' || :searchSurname || '%%')
                else
                    true
            end
            and
            -- chatId
            case
                when :chatId is null
                    then true
                when :exclude is not true
                    then
                        exists (
                            select
                                1
                            from
                                "UsersChats" uc
                            where
                                uc."chatId" = :chatId
                                and uc."userId" = u."id"
                        )
                else
                    not exists (
                        select
                            1
                        from
                            "UsersChats" uc
                        where
                            uc."chatId" = :chatId
                            and uc."userId" = u."id"
                    )
            end
            and (
                :networkStatus is null
                or (
                    case :networkStatus::smallint
                        when :online::smallint then
                            exists (
                                select
                                    1
                                from
                                    "RefreshTokens" rt
                                where
                                    rt."user" = u."id"
                                    and rt."networkStatus" = :online::smallint
                            )
                        when :offline::smallint then
                            not exists (
                                select
                                    1
                                from
                                    "RefreshTokens" rt
                                where
                                    rt."user" = u."id"
                                    and rt."networkStatus" = :online::smallint
                            )
                        else false
                    end
                )
            )
        order by
            u."surname" %s,
            u."id" %s
        fetch first :count rows only
    """, compareSign, compareSign, order, order);

        return namedParameterJdbcTemplate.query(query, namedParameters, new UserRowMapper());
    }
}
