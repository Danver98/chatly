package com.danver.messengerserver.repositories.implementations;

import com.danver.messengerserver.models.Chat;
import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.models.util.Direction;
import com.danver.messengerserver.repositories.interfaces.ChatRepository;
import com.danver.messengerserver.repositories.mappers.ChatListLightRowMapper;
import com.danver.messengerserver.repositories.mappers.ChatListRowMapper;
import com.danver.messengerserver.repositories.mappers.ChatRowMapper;
import com.danver.messengerserver.repositories.mappers.UserRowMapper;
import com.danver.messengerserver.utils.Constants;
import com.danver.messengerserver.utils.NetworkStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Repository
public class ChatRepositoryImpl implements ChatRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private record ChatPagingDtoProperTime(Long userId, OffsetDateTime time, Long chatId, Direction direction, Integer count) {
    }

    //Chats TABLE FIELDS:
    //id, name, avatarUrl, lastChanged, [participants], [messages]?
    // TODO: optimize queries

    @Autowired
    public ChatRepositoryImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    @Transactional
    public Chat createChat(Chat chat) {
        String query = """
        insert into
            "Chats" (name, "avatarUrl", private, draft, "authorId", "canAddUsers")
        values
            (:name, :avatar, :private, true, :authorId, :canAddUsers)
        on conflict
            do nothing
        returning id
        """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("name", chat.getName(), Types.VARCHAR);
        params.addValue("avatar", chat.getAvatarUrl(), Types.VARCHAR);
        params.addValue("private", chat.isPrivate(), Types.BOOLEAN);
        params.addValue("authorId", chat.getAuthorId(), Types.BIGINT);
        params.addValue("canAddUsers", chat.isCanAddUsers(), Types.BOOLEAN);
        Long id = namedParameterJdbcTemplate.queryForObject(query, params, Long.class);
        if (id == null) {
            return null;
        }
        chat.setId(id);
        if (chat.getParticipants() != null) {
            Long [] userIds = chat.getParticipants().toArray(new Long[0]);
            params.addValue("chatId", id);
            params.addValue("users", userIds, Types.ARRAY);
            query= """
            insert into
                "UsersChats" ("chatId", "userId")
            select
                :chatId, unnest(:users)
        """;
            namedParameterJdbcTemplate.update(query, params);
        }
        return this.getChat(id, chat.getAuthorId());
    }

    @Override
    @Transactional
    public List<Chat> getChats(long userId, Instant threshold, Long chatIdThreshold, int direction, Integer count) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("userId", userId);
        namedParameters.addValue("threshold", threshold);
        namedParameters.addValue("chatIdThreshold", chatIdThreshold);
        namedParameters.addValue("direction", direction == Direction.FUTURE.ordinal() ? '>' : '<');
        char compareSign =  direction == Direction.FUTURE.ordinal() ? '>' : '<';
        String order = direction == Direction.FUTURE.ordinal() ? "ASC" : "DESC";
        namedParameters.addValue("orderDirection", direction == Direction.FUTURE.ordinal() ? "ASC" : "DESC");
        namedParameters.addValue("count", count == null ? Integer.parseInt(Constants.CHATS_ONE_PAGE_COUNT) : count);
        Direction dir = direction == 0 ? Direction.FUTURE : Direction.PAST;
        ChatPagingDtoProperTime dto = new ChatPagingDtoProperTime(userId,
                threshold == null ? null : threshold.atOffset(ZoneOffset.UTC),
                chatIdThreshold, dir, count);
        String query = String.format("""
                with last_msgs as (
                    select
                        m."chatId",
                        m.id,
                        m.type,
                        m.value_type,
                        m.value,
                        m."lastChanged",
                        author.id "authorId",
                        author.name "authorName",
                        author.surname "authorSurname",
                        author."avatarUrl" "authorAvatarUrl"
                    from
                        "Messages" m
                    join (
                        select
                            m."chatId",
                            max(m."lastChanged") "lastChanged"
                        from
                            "Messages" m
                        group by
                            m."chatId"
                    ) max_data
                        on m."chatId" = max_data."chatId"
                        and m."lastChanged" = max_data."lastChanged"
                    join "Users" author
                        on m."authorId" = author.id
                ), private_chats as (
                    select
                        array_agg(c.id) "ids"
                    from
                        "Chats" c
                    join
                        "UsersChats" uc
                    on
                        uc."chatId" = c.id
                    WHERE
                        c.private is true
                        and uc."userId" = :userId
                ), private_chats_names as (
                    select
                        uc."chatId" "id",
                        u.name || ' ' || u.surname "name"
                    from
                        "UsersChats" uc
                    join "Users" u
                        on uc."userId" = u.id
                        and uc."userId" is distinct from :userId
                    where
                        uc."chatId" = any((select ids from private_chats)::bigint[])
                )
                select
                    c.id,
                    case
                        when c.private is true
                            then pcn.name
                        else
                            c.name
                    end "name",
                    c."avatarUrl",
                    c."lastChanged",
                    c.private,
                    c.draft,
                    -- last message in chat
                    last_msg.id "lastMsg.id",
                    last_msg."chatId" "lastMsg.chatId",
                    last_msg.type "lastMsg.type",
                    last_msg.value_type "lastMsg.valueType",
                    last_msg.value "lastMsg.value",
                    last_msg."lastChanged" "lastMsg.lastChanged",
                    last_msg."authorId" "lastMsg.authorId",
                    last_msg."authorName" "lastMsg.authorName",
                    last_msg."authorSurname" "lastMsg.authorSurname",
                    last_msg."authorAvatarUrl" "lastMsg.authorAvatarUrl",
                    -- last read message by user
                    uc."lastReadMsg" "lastReadMsgId",
                    (
                        select
                            count(*)
                        from
                            "Messages"
                        where
                            "lastChanged" >= coalesce((
                                select
                                    m."lastChanged"
                                from
                                    "Messages" m
                                where
                                    m."id" = uc."lastReadMsg"
                            ), to_timestamp(0))
                            and "Messages"."chatId" = c."id"
                            and "Messages"."id" is distinct from uc."lastReadMsg"
                            and "Messages"."authorId" is distinct from :userId
                    ) "unreadMsgCount"
                from
                    "Chats" c
                join
                    "UsersChats" uc
                on
                    uc."chatId" = c.id
                left join private_chats_names pcn
                    on c.id = pcn.id
                    and c.private is true
                LEFT JOIN LATERAL (
                    select
                        *
                    from
                        last_msgs m
                    where
                        m."chatId" = c.id
                ) last_msg
                ON TRUE
                WHERE
                    uc."userId" = :userId
                    and c.draft is not true
                AND
                    CASE
                        WHEN :time::timestamp with time zone IS NULL
                            THEN TRUE
                        ELSE
                            CASE
                                    WHEN :chatId IS NULL
                                        THEN c."lastChanged" %c :time::timestamp with time zone
                                    ELSE
                                        (c."lastChanged", c.id) %c (:time::timestamp with time zone, :chatId)
                            END
                    END
                ORDER BY
                    c."lastChanged" %s,
                    c.id
                FETCH FIRST :count ROWS ONLY
                """, compareSign, compareSign, order);
        return namedParameterJdbcTemplate.query(query, new BeanPropertySqlParameterSource(dto), new ChatListRowMapper());
    }

    @Override
    @Transactional
    public List<Chat> getChatsLight(long userId) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("userId", userId);
        String query = """
                select
                    c.id,
                    c.private,
                    c.draft
                from
                    "Chats" c
                join
                    "UsersChats" uc
                on
                    uc."chatId" = c.id
                WHERE
                    uc."userId" = :userId
                    and c.draft is not true
                ORDER BY
                    c."lastChanged" DESC
                """;
        return namedParameterJdbcTemplate.query(query, namedParameters, new ChatListLightRowMapper());
    }

    @Override
    @Transactional
    public List<Chat> getChatsWithParticipants(long userId) {
        return null;
    }

    @Override
    @Transactional
    public Chat getChat(long id, Long userId) {
        String query = """
        with participants as (
            select
                c.id "chatId",
                array_agg(uc."userId" order by uc."userId") "participants",
                array_agg(u.name || ' ' || u.surname order by uc."userId") "user_names"
            from
                "Chats" c
            join "UsersChats" uc
                on c.id = uc."chatId"
            join "Users" u
                on uc."userId" = u.id
            where
                c.id = :chatId
            group by
                c.id
        ),
        private_name as (
            select
                uc."chatId" "id",
                u.name || ' ' || u.surname "other_name"
            from
                "UsersChats" uc
            join "Users" u on uc."userId" = u.id
            where
                uc."chatId" = :chatId
                and :userId is not null
                and uc."userId" is distinct from :userId
            limit 1
        )
        select
            c.*,
            case c."private"
                when true
                    then pts."participants"
                else
                    null::bigint[]
            end "participants",
            array_to_string(pts."user_names", '|') "user_names",
            case
                when c."private" is true
                    then pn."other_name"
                else
                    c."name"
            end "displayName",
            uc."lastReadMsg",
            m."lastChanged" "message.lastChanged",
            (
                select
                    count(*)
                from
                    "Messages" msg
                where
                    msg."lastChanged" >= coalesce(m."lastChanged", to_timestamp(0))
                    and msg."chatId" = c."id"
                    and msg."id" is distinct from uc."lastReadMsg"
                    and msg."authorId" is distinct from :userId
            ) "unreadMsgCount",
            cardinality(pts."participants") "usersCount",
            (
                select
                    count(distinct rt."user")
                from
                    "RefreshTokens" rt
                where
                    rt."user" = any(pts."participants")
                    and rt."networkStatus" = :online::smallint
            ) "usersOnline"
        FROM
            "Chats" c
        left join participants pts
            on c."id" = pts."chatId"
        left join private_name pn
            on c."id" = pn."id"
        left join "UsersChats" uc
            on c."id" = uc."chatId"
            and uc."userId" = :userId
        left join "Messages" m
            on uc."lastReadMsg" = m."id"
        WHERE
            c."id" = :chatId
    """;
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("chatId", id, Types.BIGINT);
        namedParameters.addValue("userId", userId, Types.BIGINT);
        namedParameters.addValue("online", (short) NetworkStatus.ONLINE.toValue(), Types.SMALLINT);
        try {
            return namedParameterJdbcTemplate.queryForObject(query, namedParameters, new ChatRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<User> getParticipants(long id) {
        String query = """
            select
                *
            from
                "Users"
            where
                "id" = any((
                    select
                        array_agg("userId")
                    from
                        "UsersChats"
                    where
                        "chatId" = ?::bigint
                )::bigint[])
        """;
        return jdbcTemplate.query(query, new UserRowMapper(), id);
    }

    @Override
    @Transactional
    public void updateChat(Chat chat) {
        String query = "UPDATE \"Chats\" SET name = ?, \"avatarUrl\" = ?, private = ?, \"canAddUsers\" = ? WHERE id = ?";
        jdbcTemplate.update(query, chat.getName(), chat.getAvatarUrl(), chat.isPrivate(), chat.isCanAddUsers(), chat.getId());
//        query = "DELETE FROM \"UsersChats\" WHERE \"chatId\" = ?";
//        jdbcTemplate.update(query, chat.getId());
/*        query = "INSERT INTO UsersChats VALUES (?, ?)";
        List<Object[]> usersChats = chat.getParticipants().stream()
                .map(user -> new Object[]{user.getId(), chat.getId()}).collect(Collectors.toList());
        jdbcTemplate.batchUpdate(query, usersChats);*/
    }

    @Override
    public void updateLastReadMsg(long chatId, long userId, String messageId) {
        String query = """
            update
                "UsersChats"
            set
                "lastReadMsg" = :messageId::uuid
            where
                "chatId" = :chatId
                and "userId" = :userId
        """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("chatId", chatId, Types.BIGINT);
        params.addValue("userId", userId, Types.BIGINT);
        params.addValue("messageId", messageId, Types.VARCHAR);
        this.namedParameterJdbcTemplate.update(query, params);
    }

    @Override
    public void updateLastReadMsgForDeleted(List<Message> messages) {
        String query = """
                with data as (
                    select distinct on (uc."userId")
                        uc."userId",
                        newLastRead."id" "newLastRead"
                    from
                        "UsersChats" uc
                    join "Messages" msg
                        on uc."lastReadMsg" = msg."id"
                    left join "Messages" newLastRead
                        on newLastRead."lastChanged" <= msg."lastChanged"
                        and newLastRead."id" != msg."id"
                    where
                        uc."lastReadMsg" = any(:ids::uuid[])
                        and uc."chatId" = :chatId
                        and newLastRead.id != all(:ids::uuid[])
                    order by
                        uc."userId",
                        newLastRead."lastChanged" desc
                )
                update
                    "UsersChats"
                set
                    "lastReadMsg" = data."newLastRead"
                from
                    data
                where
                    "UsersChats"."userId" = data."userId"
            """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("chatId", messages.getFirst().getChatId());
        params.addValue("ids", messages.stream().map(Message::getId).map(String::valueOf).toArray(String[]::new));
        this.namedParameterJdbcTemplate.update(query, params);
    }

    @Override
    @Transactional
    public void deleteChat(long id) {
        String query = "DELETE FROM \"Chats\" WHERE id = ?";
        jdbcTemplate.update(query, id);
    }

    @Override
    public Chat exists(Long[] userIds) {
        if (userIds.length < 2) return null;
        if (Objects.equals(userIds[0], userIds[1])) return null;
        String query = """
            with common_chats as (
                select
                    "chatId"
                from
                    "UsersChats"
                where
                    "userId" is not distinct from :first
                
                intersect
                
                select
                    "chatId"
                from
                    "UsersChats"
                where
                    "userId" is not distinct from :second
            )
            select
                1
            from
                "Chats" c
            where
                c.private is true
                and c.id = any ((select array_agg(chatId) from common_chats )::bigint[])
            limit
                1
        """;
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("first", userIds[0], Types.BIGINT);
        namedParameters.addValue("second", userIds[1], Types.BIGINT);
        Long chat = namedParameterJdbcTemplate.queryForObject(query, namedParameters, Long.class);
        if (chat != null) {
            return this.getChat(chat, null);
        }
        return null;
    }

    @Override
    public Chat getOrCreate(Chat chat) {
        Chat existing = this.getChat(chat.getId(), chat.getAuthorId());
        if (existing != null) return existing;
        return this.createChat(chat);
    }

    @Override
    public void addParticipants(long chatId, long[] users, String messageId) {
        Long[] userIds = Arrays.stream(users).boxed().toArray(Long[]::new);

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("chatId", chatId);
        namedParameters.addValue("users", userIds, Types.ARRAY); //, Types.ARRAY
        namedParameters.addValue("messageId", messageId, Types.VARCHAR);

        String query = """
        insert into
            "UsersChats" ("chatId", "userId", "lastReadMsg")
        select
            :chatId, unnest(:users), :messageId::uuid
        on conflict
            ("chatId", "userId") do nothing
        """;

        namedParameterJdbcTemplate.update(query, namedParameters);
    }

    @Override
    public Chat getAllUsersChat() {
        Long allChatId = this.jdbcTemplate.queryForObject("""
                    select all_users_chat_id()
                """, Long.class);
        if (allChatId == null) {
            return null;
        }
        return this.getChat(allChatId, null);
    }

    @Override
    public void deleteParticipants(long chatId, long[] userIds) {
        if (userIds == null || userIds.length == 0) {
            return;
        }

        Long[] longUserIds = Arrays.stream(userIds).boxed().toArray(Long[]::new);

        this.jdbcTemplate.update("""
            delete from
                "UsersChats"
            where
                "chatId" = ?
                and "userId" = any(?)
        """, chatId, longUserIds);
    }

    private List<Long> getChats(Long userId){
        return jdbcTemplate.queryForList("""
                select distinct
                    "chatId"
                from
                    "UsersChats"
                where
                    "userId" = ?
        """, Long.class, userId);
    }

}
