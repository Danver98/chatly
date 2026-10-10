package com.danver.messengerserver.utils;

public class Constants {
    public static final String USER_JWT_LOGIN_KEY = "login";
    public static final String CHATS_ONE_PAGE_COUNT = "50";
    public static final String MESSAGE_BROKER_DESTINATION_PREFIX = "/ws";

    // Path prefix for incoming requests to be processed by methods with @MessageMapping annotation
    public static final String MESSAGE_BROKER_APPLICATION_DESTINATION_PREFIX = "/app";

    // Path prefix where we send back messages for topics (public paths)
    public static final String MESSAGE_BROKER_TOPIC_PREFIX = "/topic";
    // Path prefix where we send back messages for topics (private paths)
    public static final String MESSAGE_BROKER_QUEUE_PREFIX = "/queue";
    public static final String MESSAGE_BROKER_USER_DESTINATION_PREFIX ="/user";

    public static final String MESSAGE_BROKER_PRIVATE_CHAT_QUEUE_NAME = "/topic/chat/private/queue/messages";

    public static final String MESSAGE_BROKER_CHAT_QUEUE_NAME = "/topic/chat/queue/messages";

    public static final String REDIS_USERS_PERMISSIONS = "messenger-service:users:permissions";

    public static final String X_REQUEST_RESOURCE_OBJECT = "X-request-resource-object";

    public static final String X_REFRESH_TOKEN_COOKIE = "x-refresh-token";

    public static final String X_USER_DEVICE_ID = "X-user-device-id";

    public static final String REFRESH_TOKEN_TYPE = "REFRESH_TOKEN";

    public static final String JWT_TYPE_KEY = "type";

    public static final String ACCESS_TOKEN_BLACKLIST_KEY = "accessTokenBlackList";

}
