package com.danver.messengerserver.repositories.implementations;

import com.danver.messengerserver.models.User;
import com.danver.messengerserver.models.UserRequestDTO;
import com.danver.messengerserver.models.UserRequestFilter;
import com.danver.messengerserver.models.util.Direction;
import com.danver.messengerserver.models.util.PagingNavigation;
import com.danver.messengerserver.models.util.SortDirection;
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
import java.util.*;

@Slf4j
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private record QueryData(String condition, String order) {};

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
                    ) "networkStatus",
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
                ) "networkStatus",
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

    private QueryData getQueryData(UserRequestDTO dto) {
        String condition = "";
        String order = "";
        Map<String, SortDirection> sorting = dto.getSorting();
        PagingNavigation navigation = dto.getNavigation();

        if (navigation == null || navigation.getCursor() == null) {
            if (sorting == null || sorting.isEmpty()) return new QueryData(condition, order);
            order += "order by\n";
            List<String> sortOrders = new ArrayList<>();
            sorting.forEach((key, value) -> {
                if (key.equals("networkStatus")) {
                    // Page 1 has no CTE `u` — networkStatus/lastVisited are SELECT aliases,
                    // so reference them unqualified. id is a real column, so prefix with u.
                    sortOrders.add("\"networkStatus\" " + value.name());
                    sortOrders.add("\"lastVisited\" " + value.name() + " NULLS LAST");
                    sortOrders.add("u.\"id\" " + value.name());
                } else {
                    sortOrders.add("u.\"" + key + "\" " + value.name());
                    sortOrders.add("u.\"id\" " + value.name());
                }
            });
            order += String.join(",\n\t", sortOrders);
            return new QueryData(condition, order);
        }

        // Subsequent pages: no sorting info -> fall back to id-only pagination.
        if (sorting == null || sorting.isEmpty()) {
            condition += """
                    u."id" > cr."id"
                """;
            order += "order by\n\tu.\"id\"";
            return new QueryData(condition, order);
        }

        // Subsequent pages: build the keyset (cursor) condition and the ORDER BY
        // from the same sort map. Both must reference the CTE `u` (the main query
        // is wrapped in `u` so the computed networkStatus/lastVisited columns are
        // real columns in scope).
        List<String> conditions = new ArrayList<>();
        List<String> sortOrders = new ArrayList<>();
        sorting.forEach((key, value) -> {
            char sign = value == SortDirection.ASC ? '>' : '<';
            if (key.equals("networkStatus")) {
                // NOTE: `u."lastVisited"` is already coalesced to '-infinity'
                // inside the CTE, so the tuple comparison is NULL-safe and
                // matches the ORDER BY below.
                conditions.add(String.format("""
                        (u."networkStatus", u."lastVisited", u."id") %c (cr."networkStatus", cr."lastVisited", cr."id")
                    """, sign));
                sortOrders.add("u.\"networkStatus\" " + value.name());
                sortOrders.add("u.\"lastVisited\" " + value.name());
                sortOrders.add("u.\"id\" " + value.name());
            } else {
                // TODO: extend cursor_record with the sortable column(s) if
                // pagination on non-networkStatus keys is needed. As written,
                // cursor_record only exposes id/networkStatus/lastVisited, so
                // comparing cr."<key>" here would reference a non-existent column.
                conditions.add(String.format("""
                        (u."id") %c (cr."id")
                    """, sign));
                sortOrders.add("u.\"" + key + "\" " + value.name());
                sortOrders.add("u.\"id\" " + value.name());
            }
        });
        order = "order by\n\t";
        return new QueryData(String.join("\n\tand ", conditions), order + String.join(",\n\t", sortOrders));
    }

    @Override
    public List<User> list(UserRequestDTO dto) {
        String[] params;
        UserRequestFilter filter = dto.getFilter();
        String search = filter.getSearch();
        if (search == null || search.isEmpty()) {
            params = new String[]{null, null};
        } else {
            params = search.split(" ");
        }
        Long[] ids = null;
        if (filter.getIds() != null && filter.getIds().length > 0) {
            ids = Arrays.stream(filter.getIds())
                    .boxed()
                    .toArray(Long[]::new);
        }
        PagingNavigation nav = dto.getNavigation();
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("name", params[0], Types.VARCHAR);
        namedParameters.addValue("searchSurname", params.length > 1 ? params[1] : null, Types.VARCHAR);
        namedParameters.addValue("chatId", filter.getChatId(), Types.BIGINT);
        namedParameters.addValue("exclude", filter.getExclude(), Types.BOOLEAN);
        Integer maxPageSize = null;
        if (filter.getCount() != null) {
            maxPageSize = filter.getCount();
        } else if (nav != null && nav.getMaxPageSize() != null) {
            maxPageSize = nav.getMaxPageSize();
        }
        namedParameters.addValue("maxPageSize", maxPageSize, Types.INTEGER);
        namedParameters.addValue("cursorId", dto.getId(), Types.BIGINT);
        Long cursor = null;
        if (nav != null && nav.getCursor() != null) {
            cursor = Long.valueOf((nav.getCursor()));
        }
        namedParameters.addValue("cursor", cursor, Types.BIGINT);
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
        QueryData queryData = getQueryData(dto);
        String query = "";

        // Page 1 has no cursor_record, subsequent pages do. Both then reference `u`.
        if (cursor != null) {
            query += """
        with cursor_record as (
            select
                u."id",
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
                ) "networkStatus",
                coalesce(
                    (
                        select
                            max(rt."statusUpdateTime")
                        from
                            "RefreshTokens" rt
                        where
                            rt."user" = u."id"
                    ),
                    '-infinity'::timestamp with time zone
                ) "lastVisited"
            from
                "Users" u
            where
                u."id" = :cursor
        ),
        """;
        } else {
            query += "with ";
        }

        // Main query. Wrapped in `u` so networkStatus/lastVisited are real columns
        // that the WHERE clause and ORDER BY can reference without alias ambiguity.
        // lastVisited is coalesced to '-infinity' so NULLs sort and compare
        // consistently; the outer select turns '-infinity' back into NULL for the
        // API response.
        query += """
        u as (
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
                ) "networkStatus",
                coalesce(
                    (
                        select
                            max(rt."statusUpdateTime")
                        from
                            "RefreshTokens" rt
                        where
                            rt."user" = u."id"
                    ),
                    '-infinity'::timestamp with time zone
                ) "lastVisited"
            from
                "Users" u
        )
        select
            u.id,
            u.name,
            u.surname,
            u.email,
            u."avatarUrl",
            u."passwordHash",
            u."networkStatus",
            nullif(u."lastVisited", '-infinity'::timestamp with time zone) "lastVisited"
        from
            u
        """;
        if (cursor != null) {
            query += "left join cursor_record cr\n\ton true\n";
        }
        query += "where";
        if (cursor != null) {
            // If the cursor user was deleted between pages, cursor_record is empty
            // and we should return an empty page instead of looping back to page 1.
            query += """
                exists (select 1 from cursor_record)
            and
            """;
        }
        if (!queryData.condition.isEmpty()) {
            query += "\n" + queryData.condition + "\tand\n";
        }
        // NOTE: every reference below is `u.` — `u` is not in scope in the outer
        // query. These are independent filters (`cursorId`, `ids`) and are kept
        // exactly as before; they only kick in when their parameters are non-null.
        query += String.format("""
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
    """, compareSign, compareSign);
        if (!queryData.order.isEmpty()) {
            query += queryData.order;
        }
        if (maxPageSize != null) {
            query += "\nfetch first " + maxPageSize + " rows only";
        }

        return namedParameterJdbcTemplate.query(query, namedParameters, new UserRowMapper());
    }
}
