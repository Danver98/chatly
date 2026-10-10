package com.danver.messengerserver.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestFilter {

    /**
     * Includes name or/and surname of the user or the login
     */
    private String search;
    /**
     * A chat to take/exclude users from
     */
    private Long chatId;

    /**
     * Whether to exclude users who are in chat with chatId from the search
     */
    private Boolean exclude;

    /**
     * Number of records to fetch
     */
    private Long count;

    /**
     * Array of user identifiers
     */
    private long [] ids;
}
