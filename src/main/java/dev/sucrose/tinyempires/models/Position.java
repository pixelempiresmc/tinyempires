package dev.sucrose.tinyempires.models;

import org.bson.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Position {

    private final Set<Permission> permissions = new HashSet<>();

    public Position() {}

    public Position(List<String> permissions) {
        for (String permission : permissions)
            this.permissions.add(Permission.valueOf(permission));
    }

    public boolean hasPermission(Permission permission) {
        for (Permission p : permissions) {
            if (p == Permission.ADMIN
                    || p == permission)
                return true;
        }
        return false;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void givePermission(Permission permission) {
        permissions.add(permission);
    }

    public void revokePermission(Permission permission) {
        permissions.remove(permission);
    }

    public List<String> toList() {
        return permissions.stream()
            .map(Permission::name)
            .collect(Collectors.toList());
    }

}
