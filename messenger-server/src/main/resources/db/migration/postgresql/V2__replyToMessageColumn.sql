ALTER TABLE "Messages"
    ADD COLUMN "replyTo" uuid
        REFERENCES "Messages" ("id") ON DELETE SET NULL ON UPDATE CASCADE;