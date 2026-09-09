package com.BeSpoke.service;

import com.BeSpoke.entity.*;
import java.util.HashSet;
import java.util.Set;

/** Explicit reporting lines take precedence. Unconfigured lines follow the company role tree. */
public final class WorkHierarchy {
    private WorkHierarchy() {}
    public static boolean activeColleague(User actor, User target) {
        return target != null && target.isActive() && target.getRole().isStaff()
                && target.getCompany() != null && actor.getCompany() != null
                && actor.getCompany().getId().equals(target.getCompany().getId())
                && target.getCompany().isActive()
                && target.getCompany().effectiveEnabledRoles().contains(target.getRole());
    }
    public static boolean canAssign(User actor, User target) {
        if (!activeColleague(actor, actor) || !activeColleague(actor, target)) return false;
        if (actor.getId().equals(target.getId())) return true;
        Set<Long> visited = new HashSet<>();
        User cursor = target;
        while (cursor != null && visited.add(cursor.getId())) {
            User parent = cursor.getReportsTo();
            if (parent != null) {
                if (!activeColleague(actor, parent)) return false;
                if (parent.getId().equals(actor.getId())) return true;
                cursor = parent;
            } else {
                Set<Role> roles = new HashSet<>();
                Role role = cursor.getRole().reportsTo(actor.getCompany().getType());
                while (role != null && roles.add(role)) {
                    if (role == actor.getRole()) return true;
                    role = role.reportsTo(actor.getCompany().getType());
                }
                return false;
            }
        }
        return false;
    }
}
