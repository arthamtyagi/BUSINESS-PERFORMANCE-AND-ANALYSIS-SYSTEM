package com.vynex.repository;

import com.vynex.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.Optional;

/**
 * Raw JDBC repository — no JPA/Hibernate.
 * Uses java.sql.Connection, PreparedStatement, and ResultSet directly.
 */
@Repository
public class UserRepository {

    private final String dbUrl;

    public UserRepository(@Value("${spring.datasource.url}") String dbUrl) {
        this.dbUrl = dbUrl;
        initDB();
    }

    // ── Create table if it doesn't exist ──────────────────────────────────────
    private void initDB() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "fullName TEXT," +
                "email TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL," +
                "accountType TEXT," +
                "companyName TEXT," +
                "businessType TEXT," +
                "companySize INTEGER," +
                "industry TEXT)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("[JDBC] Database table ready.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialise database", e);
        }
    }

    // ── Check if email already exists ─────────────────────────────────────────
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            throw new RuntimeException("DB error checking email", e);
        }
    }

    // ── Insert a new user ──────────────────────────────────────────────────────
    public void save(User user) {
        String sql = "INSERT INTO users " +
                "(fullName, email, password, accountType, companyName, businessType, companySize, industry) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getAccountType());
            ps.setString(5, user.getCompanyName());
            ps.setString(6, user.getBusinessType());
            ps.setInt   (7, user.getCompanySize() != null ? user.getCompanySize() : 0);
            ps.setString(8, user.getIndustry());
            ps.executeUpdate();

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new RuntimeException("UNIQUE_EMAIL");
            }
            throw new RuntimeException("DB error saving user", e);
        }
    }

    // ── Find user by email + password (signin) ────────────────────────────────
    public Optional<User> findByEmailAndPassword(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setFullName(rs.getString("fullName"));
                user.setEmail(rs.getString("email"));
                user.setAccountType(rs.getString("accountType"));
                return Optional.of(user);
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("DB error during signin", e);
        }
    }
}
