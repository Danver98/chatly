package com.danver.messengerserver.services.permission;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface IPermissionRepository<U extends UserDetails, R> {

    /**
     *
     * @param principal
     * @param resource
     * @param resourceType
     * @return permissions for user for given resource object
     */
    List<String> getPermissions(U principal, R resource, int resourceType);

    /**
     *
     * @param principal
     * @param resource
     * @param resourceType
     * @param permission
     * @return negative value if operation failed, zero or positive number otherwise
     */
    int addPermission(U principal, R resource, int resourceType, String permission);

    int addPermission(Long user, R resource, int resourceType, String permission);

    int addPermission(List<Long> users, R resource, int resourceType, String permission);

    int addPermission(long[] userIds, R resource, int resourceType, String permission);

    int addPermission(long[] userIds, long resourceId, int resourceType, List<String> permissions);

    default int deletePermission(U principal, R resource, int resourceType, String permission) {
        return 0;
    }

    default int deletePermission(List<U> principal, R resource, int resourceType, String permission) {
        return 0;
    }

    default int deletePermissions(U principal, R resource, int resourceType) {
        return 0;
    }

    default int deletePermissions(List<U> principal, R resource, int resourceType, List<String> permission) {
        return 0;
    }

    int deletePermissions(long[] userIds, R resource, int resourceType);

    default int deletePermission(long[] userIds, R resource, int resourceType, String permission) {
        return 0;
    }

    default int deletePermissions(long[] userIds, R resource, int resourceType, List<String> permission) {
        return 0;
    }

    public int deletePermission(List<Long> users, Long resource, int resourceType, String permission);

}
