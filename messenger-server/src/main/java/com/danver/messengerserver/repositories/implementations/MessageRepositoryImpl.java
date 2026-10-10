package com.danver.messengerserver.repositories.implementations;

import com.danver.messengerserver.models.Message;
import com.danver.messengerserver.models.MessageRequestDTO;
import com.danver.messengerserver.models.util.Direction;
import com.danver.messengerserver.repositories.interfaces.MessageRepository;
import com.danver.messengerserver.repositories.mappers.MessageRowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Repository
public class MessageRepositoryImpl implements MessageRepository {

    private record MessageRequestDtoProperTime(
            Long chatId, Long userId, OffsetDateTime time, String messageId, Direction direction, Integer count) {

    }

    private static final Logger logger = LoggerFactory.getLogger(MessageRepositoryImpl.class.getName());
    private final JdbcTemplate jdbcTemplate;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public MessageRepositoryImpl(JdbcTemplate jdbcTemplate,
                                 NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public List<Message> getMessages(MessageRequestDTO dto) {
        if (dto.getTime() == null) {
            // User's just opened chat, first fetch's needed
            return this.getMessagesFirst(dto);
        }
        logger.info("Getting messages for user with id:  for chat with id {} using paging", dto.getChatId());
        MessageRequestDtoProperTime dtoProper = new MessageRequestDtoProperTime(
                dto.getChatId(), dto.getUserId(), dto.getTime() == null ? null : dto.getTime().atOffset(ZoneOffset.UTC),
                dto.getMessageId(), dto.getDirection(), dto.getCount());
        char compareSign = dto.getDirection() == Direction.FUTURE ? '>' : '<';
        String order = dto.getDirection() == Direction.FUTURE ? "ASC" : "DESC";
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(Objects.requireNonNull(this.jdbcTemplate.getDataSource()));
        String query = String.format("""
                SELECT
                    "Messages".id,
                    "Messages"."authorId",
                    "Users"."name",
                    "Users".surname,
                    "Users"."avatarUrl",
                    "Messages"."chatId",
                    "Messages".type,
                    "Messages".value,
                    "Messages".value_type,
                    "Messages"."lastChanged",
                    true::boolean "read",
                    "Messages"."replyTo",
                    reply."type" "replyType",
                    reply."value" "replyValue",
                    reply."value_type" "replyValueType",
                    replyUser."name" "replyAuthorName",
                    replyUser."surname" "replyAuthorSurname"
                FROM
                    "Messages"
                INNER JOIN "Users"
                    ON "Messages"."authorId" = "Users".id
                LEFT JOIN "Messages" reply
                    ON "Messages"."replyTo" IS NOT NULL
                    AND "Messages"."replyTo" is not distinct from reply."id"
                LEFT JOIN "Users" replyUser
                    ON reply."authorId" = replyUser."id"
                WHERE
                    "Messages"."chatId" = :chatId
                    AND CASE
                            WHEN :time::timestamp with time zone IS NULL
                                THEN TRUE
                            ELSE
                                CASE
                                    WHEN :messageId::uuid IS NULL
                                        THEN "Messages"."lastChanged" %c :time::timestamp with time zone
                                    ELSE
                                        ("Messages"."lastChanged", "Messages".id) %c (:time::timestamp with time zone, :messageId::uuid)
                                END
                        END
                ORDER BY
                    "Messages"."lastChanged" %s
                FETCH FIRST :count ROWS ONLY
                """, compareSign, compareSign, order);

        return namedParameterJdbcTemplate.query(query, new BeanPropertySqlParameterSource(dtoProper), new MessageRowMapper());
    }

    @Override
    public List<Message> getMessagesLight(String[] messageIds) {
        logger.info("Getting messages light list");
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(Objects.requireNonNull(this.jdbcTemplate.getDataSource()));
        namedParameters.addValue("ids", messageIds, Types.ARRAY);
        String query = String.format("""
                select
                    "Messages".id,
                    "Messages""authorId",
                    "Users"."name",
                    "Users".surname,
                    "Users"."avatarUrl",
                    "Messages"."chatId",
                    "Messages".type,
                    "Messages".value,
                    "Messages".value_type,
                    "Messages"."lastChanged",
                    true::boolean "read",
                    "Messages"."replyTo"
                from
                    "Messages"
                inner join "Users"
                    ON "Messages"."authorId" = "Users".id
                where
                    "Messages"."id" = any(:ids::uuid[])
                """);

        return namedParameterJdbcTemplate.query(query, namedParameters, new MessageRowMapper());
    }

    @Override
    public Message getLastMessage(MessageRequestDTO dto) {
        String query = """
            SELECT
                "Messages".id,
                "Messages"."authorId",
                "Users"."name",
                "Users".surname,
                "Users"."avatarUrl",
                "Messages"."chatId",
                "Messages"."type",
                "Messages"."value",
                "Messages"."value_type",
                "Messages"."lastChanged",
                null "read",
                "Messages"."replyTo",
                reply."type" "replyType",
                reply."value" "replyValue",
                reply."value_type" "replyValueType",
                replyUser."name" "replyAuthorName",
                replyUser."surname" "replyAuthorSurname"
            FROM
                "Messages"
            INNER JOIN "Users"
                ON "Messages"."authorId" = "Users".id
            LEFT JOIN "Messages" reply
                ON "Messages"."replyTo" IS NOT NULL
                AND "Messages"."replyTo" is not distinct from reply."id"
            LEFT JOIN "Users" replyUser
                ON reply."authorId" = replyUser."id"
            WHERE
                "Messages"."chatId" = :chatId
            ORDER BY
                "Messages"."lastChanged" desc
            LIMIT
                1
        """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("chatId", dto.getChatId(), Types.BIGINT);
        return this.namedParameterJdbcTemplate.queryForObject(query, params, new MessageRowMapper());
    }

    public List<Message> getMessagesFirst(MessageRequestDTO dto) {
        logger.info("Getting messages for user with id: {} for chat with id {} using paging", dto.getUserId(), dto.getChatId());
        MessageRequestDtoProperTime dtoProper = new MessageRequestDtoProperTime(
                dto.getChatId(), dto.getUserId(), dto.getTime() == null ? null : dto.getTime().atOffset(ZoneOffset.UTC),
                dto.getMessageId(), dto.getDirection(), dto.getCount());
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(Objects.requireNonNull(this.jdbcTemplate.getDataSource()));
        String query = """
                with last_read_msg as (
                    select
                        "id",
                        "lastChanged"
                    from
                        "Messages"
                    where
                        "id" = (
                            select
                                "lastReadMsg"
                            from
                                "UsersChats"
                            where
                                "userId" = :userId
                                and "chatId" = :chatId
                        )
                ),
                -- First fetch when opening chat (:time is null)
                -- 1 step: get messages from "lastReadMsg" to latest one;
                -- 2 step: get :count messages before "lastReadMsg".
                unread_msgs as (
                    select
                        "Messages".id,
                        "Messages"."authorId",
                        "Users"."name",
                        "Users".surname,
                        "Users"."avatarUrl",
                        "Messages"."chatId",
                        "Messages"."type",
                        "Messages"."value",
                        "Messages"."value_type",
                        "Messages"."lastChanged",
                        false::boolean "read",
                        "Messages"."replyTo",
                        reply."type" "replyType",
                        reply."value" "replyValue",
                        reply."value_type" "replyValueType",
                        replyUser."name" "replyAuthorName",
                        replyUser."surname" "replyAuthorSurname"
                    from
                        "Messages"
                    inner join "Users"
                        ON "Messages"."authorId" = "Users".id
                    left join "Messages" reply
                        on "Messages"."replyTo" is not null
                        and "Messages"."replyTo" is not distinct from reply."id"
                    left join "Users" replyUser
                        on reply."authorId" = replyUser."id"
                    where
                        "Messages"."chatId" = :chatId
                        and :time::timestamp with time zone IS NULL
                        and "Messages"."id" is distinct from (
                            select
                                "id"
                            from
                                last_read_msg
                        )
                        and "Messages"."lastChanged" >= coalesce((
                            select
                                "lastChanged"
                            from
                                last_read_msg
                        ), to_timestamp(0))
                ),
                read_msgs as (
                    select
                        "Messages".id,
                        "Messages"."authorId",
                        "Users"."name",
                        "Users".surname,
                        "Users"."avatarUrl",
                        "Messages"."chatId",
                        "Messages"."type",
                        "Messages"."value",
                        "Messages"."value_type",
                        "Messages"."lastChanged",
                        true::boolean "read",
                        "Messages"."replyTo",
                        reply."type" "replyType",
                        reply."value" "replyValue",
                        reply."value_type" "replyValueType",
                        replyUser."name" "replyAuthorName",
                        replyUser."surname" "replyAuthorSurname"
                    from
                        "Messages"
                    inner join "Users"
                        on "Messages"."authorId" = "Users".id
                    left join "Messages" reply
                        on "Messages"."replyTo" is not null
                        and "Messages"."replyTo" is not distinct from reply."id"
                    left join "Users" replyUser
                        on reply."authorId" = replyUser."id"
                    where
                        "Messages"."chatId" = :chatId
                        and ("Messages"."id" is not distinct from (
                            select
                                "id"
                            from
                                last_read_msg
                        )
                        or "Messages"."lastChanged" < coalesce((
                            select
                                "lastChanged"
                            from
                                last_read_msg
                        ), to_timestamp(0)))
                    order by
                        "Messages"."lastChanged" desc
                    fetch first :count rows only
                )
                select
                    *
                from
                    unread_msgs
                
                union

                select
                    *
                from
                    read_msgs
                order by
                    "lastChanged" desc
                """;

        return namedParameterJdbcTemplate.query(query, new BeanPropertySqlParameterSource(dtoProper), new MessageRowMapper());
    }

    @Override
    public void createMessage(Message message) {
        if (message == null) return;
        logger.info("Writing message with id: {} id to database", message.getId());
        String query = """
                    insert into
                        "Messages" (id, "chatId", "authorId", "lastChanged", value, type, value_type, "replyTo")
                    values
                        (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        UUID replyTo = null;
        if (message.getReplyTo() != null) {
            replyTo = UUID.fromString(message.getReplyTo());
        }
        this.jdbcTemplate.update(query,
                UUID.fromString(message.getId()),
                message.getChatId(),
                message.getAuthor().getId(),
                message.getTime().atOffset(ZoneOffset.UTC),
                message.getData().getValue(), //carefully!
                message.getType().ordinal() + 1,
                message.getData().getType().ordinal() + 1,
                replyTo
        );
    }

    @Override
    public void deleteMessages(List<Message> messages) {
        String query = """
                delete from
                    "Messages"
                where
                    "id" = any(?);
                """;
        jdbcTemplate.update(query, messages.stream().map(Message::getId).toArray());
    }
}
