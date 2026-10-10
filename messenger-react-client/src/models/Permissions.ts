export const Permissions = {
    Chat: {
        ADMIN: 'Chat.Admin',
        ANY: 'Chat.Any',
        /*
            DEFAULT - can read/send messages and make basic actions as a chat participant
         */
        DEFAULT: 'Chat.Default',

        User: {
            ADD: 'Chat.User.Add',
            DELETE: 'Chat.User.Delete'
        }
    }
} as const