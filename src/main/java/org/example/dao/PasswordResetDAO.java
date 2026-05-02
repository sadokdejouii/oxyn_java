package org.example.dao;

import org.example.utils.MyDataBase;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

/**
 * Stocke des codes de réinitialisation (OTP) envoyés par e-mail.
 * Un code est valable quelques minutes et ne peut être utilisé qu'une seule fois.
 */
public final class PasswordResetDAO {

    private static final SecureRandom RNG = new SecureRandom();

    private final Connection con;

    public PasswordResetDAO() {
        this.con = MyDataBase.getInstance().getConnection();
        try {
            ensureTable();
        } catch (SQLException e) {
            System.err.println("[PasswordResetDAO] ensureTable failed: " + e.getMessage());
        }
    }

    public void ensureTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS password_reset_codes (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    email_user VARCHAR(255) NOT NULL,
                    code_hash VARCHAR(64) NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    used TINYINT(1) NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_prc_email (email_user),
                    INDEX idx_prc_expires (expires_at)
                )
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.execute();
        }
    }

    /**
     * Crée un nouveau code pour cet e-mail (en invalidant les anciens), avec une durée de vie.
     * @return le code en clair (à envoyer par e-mail)
     */
    public String createResetCode(String email, Duration ttl) throws SQLException {
        if (email == null || email.isBlank()) {
            throw new SQLException("Email obligatoire");
        }
        Duration effectiveTtl = (ttl != null) ? ttl : Duration.ofMinutes(10);
        Instant expiresAt = Instant.now().plus(effectiveTtl);

        String code = String.format("%06d", RNG.nextInt(1_000_000));
        String codeHash = sha256Hex(code);

        // Invalider les anciens codes (même email) + insérer un nouveau
        try (PreparedStatement del = con.prepareStatement(
                "UPDATE password_reset_codes SET used = 1 WHERE LOWER(TRIM(email_user)) = LOWER(TRIM(?))")) {
            del.setString(1, email);
            del.executeUpdate();
        }
        try (PreparedStatement ins = con.prepareStatement(
                "INSERT INTO password_reset_codes (email_user, code_hash, expires_at, used) VALUES (?, ?, ?, 0)")) {
            ins.setString(1, email.trim());
            ins.setString(2, codeHash);
            ins.setTimestamp(3, java.sql.Timestamp.from(expiresAt));
            ins.executeUpdate();
        }
        return code;
    }

    /**
     * Vérifie un code (non expiré, non utilisé) et le marque comme utilisé si OK.
     */
    public boolean consumeCodeIfValid(String email, String code) throws SQLException {
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        String codeHash = sha256Hex(code.trim());

        String sql = """
                SELECT id, code_hash
                FROM password_reset_codes
                WHERE used = 0
                  AND expires_at > CURRENT_TIMESTAMP
                  AND LOWER(TRIM(email_user)) = LOWER(TRIM(?))
                ORDER BY id DESC
                LIMIT 1
                """;

        Long id = null;
        String stored = null;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    id = rs.getLong("id");
                    stored = rs.getString("code_hash");
                }
            }
        }
        if (id == null || stored == null) {
            return false;
        }
        if (!stored.equalsIgnoreCase(codeHash)) {
            return false;
        }
        try (PreparedStatement upd = con.prepareStatement(
                "UPDATE password_reset_codes SET used = 1 WHERE id = ?")) {
            upd.setLong(1, id);
            upd.executeUpdate();
        }
        return true;
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

