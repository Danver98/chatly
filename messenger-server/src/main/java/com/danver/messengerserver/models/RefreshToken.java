package com.danver.messengerserver.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
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

    @Setter
    @Getter
    @Column("networkStatus")
    private Short networkStatus;

    @Setter
    @Getter
    @Column("statusUpdateTime")
    private Instant statusUpdateTime;

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
                Objects.equals(deviceId, that.deviceId) &&
                Objects.equals(networkStatus, that.networkStatus) &&
                Objects.equals(statusUpdateTime, that.statusUpdateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, token, deviceId, networkStatus, statusUpdateTime);
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", userId=" + userId +
                ", device='" + deviceId + '\'' +
                ", token='" + token + '\'' +
                ", networkStatus=" + networkStatus +
                ", statusUpdateTime=" + statusUpdateTime +
                '}';
    }
}