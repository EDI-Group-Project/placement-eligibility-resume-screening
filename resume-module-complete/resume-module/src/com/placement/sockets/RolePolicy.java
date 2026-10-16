package com.placement.sockets;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Server-side authorization. A client can never grant itself a role: the role comes from the SESSION that was
 * created at login ({@link SessionManager#getAccountType}), never from anything the client sends later.
 *
 * It is an ALLOW-LIST: an unknown role, a missing role or an invalid token is denied.
 *
 *   staff   = TPO, TPC, ADMIN        (view / export student data, batch upload, applicant lists)
 *   student = STUDENT                (own resume, own screening, apply)
 *
 * The strings must equal what LoginResult.getAccountType() returns (compared case-insensitively). If your
 * login uses different names, change them WITHOUT recompiling:
 *     -Dplacement.staffRoles=TPO,TPC,ADMIN,FACULTY      or env var PLACEMENT_STAFF_ROLES
 *     -Dplacement.studentRoles=STUDENT                   or env var PLACEMENT_STUDENT_ROLES
 * A wrong value fails CLOSED (staff are shown "Access denied"); it never opens access to students.
 */
public final class RolePolicy {

    public static final String STAFF_DENIED_MESSAGE = "Access denied. Only TPO, TPC or Admin can use this feature.";

    private static final Set<String> STAFF_ROLES = load("placement.staffRoles", "PLACEMENT_STAFF_ROLES", "TPO,TPC,ADMIN");
    private static final Set<String> STUDENT_ROLES = load("placement.studentRoles", "PLACEMENT_STUDENT_ROLES", "STUDENT");

    private RolePolicy() {}

    public static boolean isStaff(String token) {
        return hasRole(token, STAFF_ROLES);
    }

    /** A student account. A role that is configured as staff is never treated as a student. */
    public static boolean isStudent(String token) {
        return hasRole(token, STUDENT_ROLES) && !hasRole(token, STAFF_ROLES);
    }

    public static Set<String> staffRoles() {
        return Collections.unmodifiableSet(STAFF_ROLES);
    }

    private static boolean hasRole(String token, Set<String> allowed) {
        if (!SessionManager.isValid(token)) return false;
        String type = SessionManager.getAccountType(token);
        return type != null && allowed.contains(type.trim().toUpperCase(Locale.ROOT));
    }

    private static Set<String> load(String property, String envVariable, String defaults) {
        String raw = System.getProperty(property);
        if (raw == null || raw.isBlank()) raw = System.getenv(envVariable);
        if (raw == null || raw.isBlank()) raw = defaults;
        Set<String> roles = new HashSet<>();
        for (String part : Arrays.asList(raw.split(","))) {
            String role = part.trim().toUpperCase(Locale.ROOT);
            if (!role.isEmpty()) roles.add(role);
        }
        return roles;
    }
}
