
package com.clubexpense.security;

import com.clubexpense.model.User;

import java.util.Locale;
import java.util.Objects;

/**
 * Central role-based permission policy.
 * Call these checks before displaying or executing protected operations.
 */
public final class Authorization {

    private Authorization() {
        // Utility class; do not instantiate.
    }

    private static String roleOf(User user) {
        Objects.requireNonNull(user, "User must be logged in.");

        return Objects.requireNonNull(
                user.getRole(),
                "User role is missing."
        ).trim().toUpperCase(Locale.ROOT);
    }

    private static boolean hasRole(
            User user,
            String... allowedRoles) {

        String role = roleOf(user);

        for (String allowedRole : allowedRoles) {
            if (role.equals(allowedRole)) {
                return true;
            }
        }

        return false;
    }

    public static boolean canViewDashboard(User user) {
        return user != null;
    }

    public static boolean canManageUsers(User user) {
        return hasRole(user, "ADMIN");
    }

    public static boolean canManageEvents(User user) {
        return hasRole(
                user,
                "ADMIN",
                "CLUB_COORDINATOR",
                "EVENT_COORDINATOR"
        );
    }

    public static boolean canManageParticipants(User user) {
        return hasRole(
                user,
                "ADMIN",
                "CLUB_COORDINATOR",
                "EVENT_COORDINATOR"
        );
    }

    public static boolean canManageFinances(User user) {
        return hasRole(
                user,
                "ADMIN",
                "FINANCE_COORDINATOR"
        );
    }

    public static boolean canViewFinancialReports(User user) {
        return canManageFinances(user);
    }

    // Task Management permissions.

    public static boolean canViewTasks(User user) {
        return hasRole(
                user,
                "ADMIN",
                "CLUB_COORDINATOR",
                "EVENT_COORDINATOR",
                "FINANCE_COORDINATOR"
        );
    }

    public static boolean canManageTasks(User user) {
        return hasRole(
                user,
                "ADMIN",
                "CLUB_COORDINATOR",
                "EVENT_COORDINATOR"
        );
    }

    public static void requireTaskViewing(User user) {
        require(
                canViewTasks(user),
                "You do not have permission to view tasks."
        );
    }

    public static void requireTaskManagement(User user) {
        require(
                canManageTasks(user),
                "You do not have permission to manage tasks."
        );
    }

    public static void requireUserManagement(User user) {
        require(
                canManageUsers(user),
                "Only an Admin can manage user accounts."
        );
    }

    public static void requireEventManagement(User user) {
        require(
                canManageEvents(user),
                "You do not have permission to manage events."
        );
    }

    public static void requireParticipantManagement(User user) {
        require(
                canManageParticipants(user),
                "You do not have permission to manage participants."
        );
    }

    public static void requireFinanceAccess(User user) {
        require(
                canManageFinances(user),
                "You do not have permission to access financial operations."
        );
    }

    public static void requireFinancialReports(User user) {
        require(
                canViewFinancialReports(user),
                "You do not have permission to access financial reports."
        );
    }

    private static void require(
            boolean permitted,
            String message) {

        if (!permitted) {
            throw new SecurityException(message);
        }
    }
}
