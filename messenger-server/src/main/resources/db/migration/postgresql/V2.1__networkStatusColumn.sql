ALTER TABLE "RefreshTokens"
    ADD COLUMN  "networkStatus" SMALLINT,
    ADD COLUMN "statusUpdateTime" timestamp with time zone;
