package com.danver.messengerserver.services.permission;

import org.springframework.security.core.userdetails.UserDetails;

public record PermissionData<U extends UserDetails>(U principal, Long resourceId, int resourceType, String permission) {
}
