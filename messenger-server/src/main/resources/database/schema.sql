-- Users table
CREATE TABLE IF NOT EXISTS "Users"
(
    "id"           bigserial PRIMARY KEY,
    "name"         varchar(30)  NOT NULL,
    "surname"      varchar(30)  NOT NULL,
    "email"        varchar(50)  NOT NULL UNIQUE,
    "salt"         bytea,
    "passwordHash" varchar(128) NOT NULL,
    "avatarUrl"    varchar(300)
);

CREATE OR REPLACE FUNCTION all_users_chat_id() RETURNS BIGINT AS '
BEGIN
    RETURN 6;
END;
'
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION add_user_to_all_users_chat() RETURNS TRIGGER AS '
BEGIN
    IF (TG_OP = ''INSERT'') THEN
        INSERT INTO "UsersChats" ("userId", "chatId") VALUES (NEW.id, all_users_chat_id());
        RETURN NEW;
    END IF;
END;
'
LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER add_user_to_all_users_chat_trg AFTER INSERT ON "Users"
    FOR EACH ROW EXECUTE PROCEDURE add_user_to_all_users_chat();


-- Chats table
CREATE TABLE IF NOT EXISTS "Chats"
(
    "id"        bigserial PRIMARY KEY,
    "name"      varchar(100),
    "avatarUrl" varchar(200),
    "lastChanged" timestamp with time zone, -- supposed to be the time when last message was sent
    "private" boolean,
    "draft" boolean default true,
    "canAddUsers" boolean default false
);
-- Should we create index on LastChanged if it's changed often?

-- Message table
CREATE TABLE IF NOT EXISTS "Messages"
(
    "id"           uuid PRIMARY KEY,
    "chatId"       bigint    NOT NULL references "Chats" (id) ON DELETE CASCADE ON UPDATE CASCADE,
    "authorId"     bigint    NOT NULL references "Users" (id) ON DELETE CASCADE ON UPDATE CASCADE,
    "lastChanged" timestamp with time zone NOT NULL,
    "type"        smallint,
    "value_type"  smallint,
    -- Currently we only store TEXT
    "value"        text      NOT NULL,
    "replyTo" uuid references "Messages" ("id") ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE OR REPLACE FUNCTION update_chat_last_changed_column() RETURNS TRIGGER AS '
BEGIN
    IF (TG_OP = ''INSERT'') OR (TG_OP = ''UPDATE'') THEN
        UPDATE
            "Chats"
        SET
            "lastChanged" = NEW."lastChanged",
            draft = null
        WHERE
            id = NEW."chatId";
        RETURN NEW;
    ELSE IF (TG_OP = ''DELETE'') THEN
        UPDATE
            "Chats"
        SET
            "lastChanged" = OLD."lastChanged"
            --draft = OLD.draft
        WHERE
            id = NEW."chatId";
        RETURN OLD;
    end if;
    END IF;
END;
'LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER update_chat_last_changed_column_trg AFTER INSERT OR UPDATE OR DELETE on "Messages"
    FOR EACH ROW EXECUTE PROCEDURE update_chat_last_changed_column();


-- UsersChats table
CREATE TABLE IF NOT EXISTS "UsersChats"
(
    "userId" bigint NOT NULL references "Users" (id) ON DELETE CASCADE ON UPDATE CASCADE,
    "chatId" bigint NOT NULL references "Chats" (id) ON DELETE CASCADE ON UPDATE CASCADE,
    "lastReadMsg" uuid references "Messages" (id) ON DELETE SET NULL ON UPDATE CASCADE,
    PRIMARY KEY ("userId", "chatId")
);

-- UserPermissions table
CREATE TABLE IF NOT EXISTS "UsersPermissions"
(
    "id" bigserial PRIMARY KEY,
    "user" bigint NOT NULL REFERENCES "Users" (id) ON DELETE CASCADE ON UPDATE CASCADE,
    "resource" bigint, -- references different objects from different tables
    "resource_type" smallint NOT NULL,
    "permissions" text[],
    UNIQUE ("user", "resource", "resource_type")
);

-- Refresh tokens table
CREATE TABLE IF NOT EXISTS "RefreshTokens"
(
    "id" bigserial PRIMARY KEY,
    "user" bigint NOT NULL REFERENCES "Users" (id) ON DELETE CASCADE ON UPDATE CASCADE,
    "device" text,
    "token" text not null
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_user_device
    ON "RefreshTokens" ("user", "device")
    NULLS NOT DISTINCT;




