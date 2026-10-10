package com.danver.messengerserver.models;

import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails, Principal {
    private long id;
    @JsonProperty("login")
    @JsonAlias("email")
    private String email;
    //This field can store both raw password and hashed
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private byte[] salt;
    private String name;
    private String surname;
    @JsonProperty("avatar")
    @JsonAlias("avatar")
    private String avatarUrl;
    private Set<Role> roles;
    //private List<Chat> chats;

    public enum UserRoles {
        ROLE_USER,
        ROLE_ADMIN
    }

    public User flushPasswordAndSalt() {
        this.password = null;
        this.salt = null;
        return this;
    }


    /*
    ======================== METHODS OF USERDETAILS INTERFACE ===========================
     */

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public String getUsername() {
        return this.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void setAuthorities(Set<Role> roles) {
        this.roles = roles;
    }

    public String getFullName() {
        return this.getName() + ' ' + this.getSurname();
    }
}
