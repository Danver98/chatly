ALTER TABLE "Chats"
    ADD COLUMN "authorId" INTEGER REFERENCES "Users"("id");