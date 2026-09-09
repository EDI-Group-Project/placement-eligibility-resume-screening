package com.placement.integration;

import com.placement.security.PasswordHasher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test
    void hashCanBeVerified() {
        String encoded=PasswordHasher.hashPassword("correct-horse-battery-staple");
        assertTrue(PasswordHasher.verifyPassword("correct-horse-battery-staple",encoded));
        assertFalse(PasswordHasher.verifyPassword("wrong-password",encoded));
        assertNotEquals(encoded, PasswordHasher.hashPassword("correct-horse-battery-staple"));
    }
}
