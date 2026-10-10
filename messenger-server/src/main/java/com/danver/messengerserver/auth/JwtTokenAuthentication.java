package com.danver.messengerserver.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtTokenAuthentication extends AbstractAuthenticationToken {

    private final String token;
    public JwtTokenAuthentication(String token, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.token = token;
        this.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }
}
