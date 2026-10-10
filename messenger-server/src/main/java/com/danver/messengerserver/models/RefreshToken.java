package com.danver.messengerserver.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

@Table("RefreshTokens")
public class RefreshToken {

    // Getters and Setters
    @Setter
    @Getter
    @Id
    private Long id;

    @Setter
    @Getter
    @Column("user")
    private Long userId;

    @Setter
    @Getter
    @Column("device")
    private String deviceId;

    @Setter
    @Getter
    @Column("token")
    private String token;

    // Constructors
    public RefreshToken() {}

    public RefreshToken(Long userId, String token, String deviceId) {
        this.userId = userId;
        this.token = token;
        this.deviceId = deviceId;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(token, that.token) &&
                Objects.equals(deviceId, that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, token, deviceId);
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", userId=" + userId +
                ", token='" + token + '\'' +
                ", device='" + deviceId + '\'' +
                '}';
    }
}