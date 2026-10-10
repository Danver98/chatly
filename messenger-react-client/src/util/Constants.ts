export const RANDOM_AVATAR_URL = 'https://i.pravatar.cc/'; // /{size}
export const RANDOM_CHAT_AVATAR_URL = 'https://random.imagecdn.app/v1/image' // /{width}/{height}
export const ACCESS_TOKEN = 'access_token';
export const REFRESH_TOKEN = 'refresh_token';
export const DEVICE_ID = 'device_id';
export const CURRENT_LOGGED_USER = 'current_logged_user'
export const CHATS_COMPONENT_MESSAGE_QUEUE = '/components/chats/messages';
export const CHATS_COMPONENT_MSG_UNREAD_COUNT_QUEUE = '/components/chats/messages/unread/count';

export const DIRECTION = {
    FUTURE: 1,
    PAST: 2
}

export class ServiceUrl {
    static readonly BACKEND_SERVICE_BASE_URL = import.meta.env.VITE_API_URL;
    static readonly BACKEND_SERVICE_WEB_SOCKET_URL = import .meta.env.VITE_WEB_SOCKET_API_URL;
    static readonly AUTH_BASE = '/auth';
    static readonly MESSENGER_BASE = '/chats';
    static readonly USER_BASE = '/users';
}

export class Headers {
    static readonly AUTHORIZATION = 'Authorization';
    static readonly X_REQUEST_RESOURCE_OBJECT = 'X-request-resource-object';
    static readonly X_USER_DEVICE_ID = 'X-user-device-id';
};

export class Pages {
    /**
     * Must be in sync with routing structure
     */
    static readonly LOGIN_PAGE = '/login';
    static readonly REGISTER_PAGE = '/register'
    static readonly INTRO_PAGE = '/intro';
};

export class SecuredPages {
    /**
     * Must be in sync with routing structure
    */

    static readonly ROOT = '/secured';
    static readonly CHATS_PAGE = SecuredPages.ROOT + '/chats';
    static readonly HOME_PAGE = SecuredPages.ROOT + '/home';
    // Chat dialog itself
    static readonly CHAT_ROOM_PAGE = SecuredPages.ROOT + '/chats/room';
    static readonly PROFILE_PAGE = SecuredPages.ROOT + '/profile';
    static readonly SETTINGS = SecuredPages.ROOT + '/settings';
};