package dev.sucrose.tinyempires.models;

import org.bson.Document;

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

    /**
     * Compares two positions' relative superiority
     * @param position1 First position to compare
     * @param position2 Second position to compare
     * @return 0 if both positions have the same number of permissions or are both admin,
     * 1 if position1 has admin or has more permissions, or 2 if position2 has admin or
     * more permissions
     */
    public static int compare(Position position1, Position position2) {
        final boolean position1HasAdmin = position1.hasPermission(Permission.ADMIN);
        final boolean position2HasAdmin = position2.hasPermission(Permission.ADMIN);
        if (position1.hasPermission(Permission.ADMIN)
                || position2.hasPermission(Permission.ADMIN))
            return Boolean.compare(position1HasAdmin, position2HasAdmin);
        return Integer.compare(position1.getPermissions().size(), position2.getPermissions().size());
    }

}
