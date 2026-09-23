package com.placement.integration;

import com.placement.database.DatabaseConnection;
import com.placement.models.LoginResult;
import com.placement.services.AuthService;
import com.placement.services.JobService;
import com.placement.services.StudentService;
import org.junit.jupiter.api.Test;

import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DatabaseIntegrationTest {
    private void requireDb() {
        assumeTrue("true".equalsIgnoreCase(System.getProperty("RUN_DB_TESTS")),
                "Set -DRUN_DB_TESTS=true to execute MySQL integration tests.");
    }

    @Test
    void jdbcConnectionWorks() throws Exception {
        requireDb();
        DatabaseConnection.verifyConnection();
    }

    @Test
    void seededDataIsAvailableThroughJdbc() throws Exception {
        requireDb();
        try (Connection c=DatabaseConnection.getConnection();
             PreparedStatement ps=c.prepareStatement(
                     "SELECT (SELECT COUNT(*) FROM students), " +
                     "(SELECT COUNT(*) FROM faculty), " +
                     "(SELECT COUNT(*) FROM company), " +
                     "(SELECT COUNT(*) FROM job_postings)")) {
            try(ResultSet rs=ps.executeQuery()) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1)>=5);
                assertTrue(rs.getInt(2)>=5);
                assertTrue(rs.getInt(3)>=5);
                assertTrue(rs.getInt(4)>=5);
            }
        }
    }

    @Test
    void authenticationUsesDatabaseCredentials() {
        requireDb();
        LoginResult result = new AuthService().login("TPC","neha@college.com","pass123");
        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals("Neha Deshmukh", result.getDisplayName());
    }

    @Test
    void eligibilityReadUsesDatabaseData() throws Exception {
        requireDb();
        var jobs=new JobService().getAllJobs();
        assertFalse(jobs.isEmpty());
        var students=new StudentService().getEligibleUnnotifiedStudents(jobs.get(0));
        assertNotNull(students);
    }
}
