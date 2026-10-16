package com.placement.sockets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RolePolicyTest {

    private static String session(String type) {
        return SessionManager.createSession(type, "Name", 1);
    }

    @Test
    void staffRolesAreTpoTpcAndAdmin_caseInsensitive() {
        assertTrue(RolePolicy.isStaff(session("TPO")));
        assertTrue(RolePolicy.isStaff(session("tpc")));
        assertTrue(RolePolicy.isStaff(session("Admin")));
        assertTrue(RolePolicy.isStaff(session("  ADMIN ")));
    }

    @Test
    void studentIsNeverStaff() {
        String student = session("STUDENT");
        assertFalse(RolePolicy.isStaff(student));
        assertTrue(RolePolicy.isStudent(student));
    }

    @Test
    void unknownMissingOrInvalidRolesAreDenied() {
        assertFalse(RolePolicy.isStaff(session("PARENT")));
        assertFalse(RolePolicy.isStudent(session("PARENT")));
        assertFalse(RolePolicy.isStaff(session("")));
        assertFalse(RolePolicy.isStaff(session(null)));
        assertFalse(RolePolicy.isStaff("forged-token"));
        assertFalse(RolePolicy.isStaff(null));
        assertFalse(RolePolicy.isStudent(null));
    }

    @Test
    void staffAccountIsNotAlsoAStudent() {
        String tpo = session("TPO");
        assertFalse(RolePolicy.isStudent(tpo));
    }

    @Test
    void loggedOutSessionLosesAllAccess() {
        String token = session("TPO");
        assertTrue(RolePolicy.isStaff(token));
        SessionManager.invalidate(token);
        assertFalse(RolePolicy.isStaff(token));
    }
}
