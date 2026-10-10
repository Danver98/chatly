package com.danver.messengerserver.services.permission;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface IPermissionService<U extends UserDetails, R> {

    boolean isAuthorized(U principal, R resource, int resourceType, String permission);

    /**
     *
     * @param principal
     * @param resource
     * @param resourceType
     * @return permissions for user for given resource object
     */
    List<String> getPermissions(U principal, R resource, int resourceType);

    // ==================== GRANTING AUTHORITIES ====================
    /**
     * Adds certain permissions to the list of permissions
     *
     * @param principal
     * @param resource
     * @param permission
     * @param resourceType
     * @return
     */
    default int grantAuthority(U principal, R resource, int resourceType, String permission) {
        return 0;
    }

    default int grantAuthority(Long user, R resource, int resourceType, String permission) {
        return 0;
    }

    default int grantAuthority(List<Long> users, Long resource, int resourceType, List<String> permission) {
        return 0;
    }

    int grantAuthority(List<Long> userIds, Long resource, int resourceType, String permission);

    int grantAuthority(long[] userIds, long chatId, int resourceType, String permission);

    int grantAuthority(long[] userIds, long chatId, int resourceType, List<String> permissions);

    // ==================== REVOKING AUTHORITIES ====================

    default int revokeAuthority(U principal, R resource, int resourceType, String permission) {
        return 0;
    }

    default int revokeAuthority(List<U> principal, R resource, int resourceType, List<String> permission) {
        return 0;
    }

    /**
     * Revokes ALL permissions for certain principal relating to resource
     *
     * @param principal
     * @param resource
     * @param resourceType
     * @return
     */
    default int revokeAuthority(U principal, R resource, int resourceType) {
        return 0;
    }

    default int revokeAuthority(List<U> principal, R resource, int resourceType) {
        return 0;
    }

    default int revokeAuthority(long[] users, R resource, int resourceType) {
        return 0;
    }

    int revokeAuthority(long[] userIds, long resourceId, int resourceType);

    public int revokeAuthority(List<Long> users, Long resource, int resourceType, String permission);
}
