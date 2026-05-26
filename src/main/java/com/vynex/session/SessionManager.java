package com.vynex.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SessionManager — persists sessions in SQLite so they survive server restarts.
 *
 * MULTITHREADING:
 *   - A background daemon thread runs every 60 seconds to clean expired sessions
 *   - Main thread handles login/check instantly without waiting
 */
@Component
public class SessionManager {

    private final String dbUrl;
    private final long   timeoutMinutes;

    public SessionManager(
            @Value("${spring.datasource.url}") String dbUrl,
            @Value("${session.timeout.minutes:30}") long timeoutMinutes) {

        this.dbUrl          = dbUrl;
        this.timeoutMinutes = timeoutMinutes;
        initTable();

        // Background thread — cleans expired sessions every 60 seconds
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-cleanup-thread");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::removeExpiredSessions, 60, 60, TimeUnit.SECONDS);
        System.out.println("[SessionManager] DB-backed sessions ready. Timeout: " + timeoutMinutes + " min.");
    }

    private void initTable() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS sessions (" +
                    "token TEXT PRIMARY KEY," +
                    "email TEXT," +
                    "name TEXT," +
                    "accountType TEXT," +
                    "createdAt TEXT)");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init sessions table", e);
        }
    }

    // ── Create session — stored in DB ─────────────────────────────────────────
    public String createSession(String email, String name, String accountType) {
        String token = UUID.randomUUID().toString();
        String sql   = "INSERT OR REPLACE INTO sessions (token, email, name, accountType, createdAt) VALUES (?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, email);
            ps.setString(3, name);
            ps.setString(4, accountType);
            ps.setString(5, Instant.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB error creating session", e);
        }

        System.out.println("[SessionManager] Session created for: " + email);
        return token;
    }

    // ── Check if token is valid ───────────────────────────────────────────────
    public boolean isValid(String token) {
        String sql = "SELECT createdAt FROM sessions WHERE token = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return false;

            Instant created = Instant.parse(rs.getString("createdAt"));
            boolean expired = Instant.now().isAfter(created.plusSeconds(timeoutMinutes * 60));

            if (expired) {
                invalidate(token);
                return false;
            }
            return true;

        } catch (SQLException e) {
            return false;
        }
    }

    // ── Get session data ──────────────────────────────────────────────────────
    public SessionData getSession(String token) {
        String sql = "SELECT * FROM sessions WHERE token = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new SessionData(
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getString("accountType"),
                        Instant.parse(rs.getString("createdAt")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error getting session", e);
        }
        return null;
    }

    // ── Invalidate (logout) ───────────────────────────────────────────────────
    public void invalidate(String token) {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM sessions WHERE token = ?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB error invalidating session", e);
        }
    }

    // ── Background thread: remove expired sessions ────────────────────────────
    private void removeExpiredSessions() {
        String cutoff = Instant.now().minusSeconds(timeoutMinutes * 60).toString();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM sessions WHERE createdAt < ?")) {
            ps.setString(1, cutoff);
            int removed = ps.executeUpdate();
            if (removed > 0)
                System.out.println("[session-cleanup-thread] Removed " + removed + " expired session(s).");
        } catch (SQLException e) {
            System.err.println("[session-cleanup-thread] Error: " + e.getMessage());
        }
    }

    public record SessionData(String email, String name, String accountType, Instant createdAt) {}
}
