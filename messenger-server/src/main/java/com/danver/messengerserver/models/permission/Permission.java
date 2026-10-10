package com.danver.messengerserver.models.permission;

public record Permission(long id, long user, Long resource, short resourceType, String[] permissions) {
}
