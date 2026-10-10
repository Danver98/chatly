package com.danver.messengerserver.auth;


import com.danver.messengerserver.models.User;
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
public class AuthData {
    User user;
    String accessToken;
    String refreshToken;
}
