package com.danver.messengerserver.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@NoArgsConstructor  // Required for Jackson 3
@AllArgsConstructor // Required for builder
@Jacksonized
public class AuthDTO {

    @JsonAlias({"email"})
    private String login;
    private String password;
    private String token;
    private String deviceId;
}
