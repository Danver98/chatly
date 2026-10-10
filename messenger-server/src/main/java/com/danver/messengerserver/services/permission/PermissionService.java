package com.danver.messengerserver.services.permission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PermissionService implements IPermissionService<UserDetails, Long> {

    private final IPermissionRepository<UserDetails, Long> permissionRepository;

    @Autowired
    public PermissionService(IPermissionRepository<UserDetails, Long> permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public boolean isAuthorized(UserDetails principal, Long resourceId, int resourceType, String permission) {
        List<String> permissions = permissionRepository.getPermissions(principal, resourceId, resourceType);
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        return permissions.contains(permission);
    }

    @Override
    public List<String> getPermissions(UserDetails principal, Long resource, int resourceType) {
        List<String> permissions = permissionRepository.getPermissions(principal, resource, resourceType);
        if (permissions == null) {
            return List.of();
        }
        return permissions;
    }

    @Override
    public int grantAuthority(UserDetails principal, Long resourceId, int resourceType, String permission) {
        return permissionRepository.addPermission(principal, resourceId, resourceType, permission);
    }

    @Override
    public int grantAuthority(Long user, Long resource, int resourceType, String permission) {
        return permissionRepository.addPermission(user, resource, resourceType, permission);
    }

    @Override
    public int grantAuthority(List<Long> users, Long resource, int resourceType, String permission) {
        return permissionRepository.addPermission(users, resource, resourceType, permission);
    }

    @Override
    public int grantAuthority(long[] userIds, long resourceId, int resourceType, String permission) {
        return permissionRepository.addPermission(userIds, resourceId, resourceType, permission);
    }

    @Override
    public int grantAuthority(long[] userIds, long resourceId, int resourceType, List<String> permissions) {
        return permissionRepository.addPermission(userIds, resourceId, resourceType, permissions);
    }

    @Override
    public int revokeAuthority(long[] userIds, long resourceId, int resourceType) {
        return this.permissionRepository.deletePermissions(userIds, resourceId, resourceType);
    }

    public int revokeAuthority(List<Long> users, Long resource, int resourceType, String permission) {
        return permissionRepository.deletePermission(users, resource, resourceType, permission);
    }
}
