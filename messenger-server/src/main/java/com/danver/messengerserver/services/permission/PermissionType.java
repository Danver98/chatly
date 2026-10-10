package com.danver.messengerserver.services.permission;

public enum PermissionType {
    ;
    public enum Chat {
        ADMIN("Admin"),
        ANY("Any"),
        CREATE("Create"),
        /*
            DEFAULT - can read/send messages and make basic actions as a chat participant
         */
        DEFAULT("Default"),
        EDIT("Edit"),
        DELETE("Delete"),
        LIST("List"),
        READ("Read"),
        SEND("Send");
        private final String value;
        Chat(String value) {
            this.value = value;
        }

        public String getValue() {
            return Chat.class.getSimpleName() + "." + value;
        }

        public enum User {
            ADD("Add"),
            DELETE("Delete");

            private final String value;
            User(String value) {
                this.value = value;
            }
            public String getValue() {
                return Chat.class.getSimpleName() + "." + User.class.getSimpleName() + "." + value;
            }
        }

        public enum Message {
            SEND("Send"),
            DELETE("Delete"),
            READ("Read");
            private final String value;
            Message(String value) {
                this.value = value;
            }
            public String getValue() {
                return Chat.class.getSimpleName() + "." + Message.class.getSimpleName() + "." + value;
            }
        }

    }

}
