package org.example.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Hachage et vérification BCrypt. Compatible avec les hash PHP {@code $2y$...} (normalisés en {@code $2a$} pour jBCrypt).
 */
public final class PasswordUtils {

    private static final int BCRYPT_LOG_ROUNDS = 12;

    private PasswordUtils() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_LOG_ROUNDS));
    }

    /**
     * @param plainPassword mot de passe saisi
     * @param storedHash    hash issu de la base (ex. PHP {@code $2y$13$...} ou Java {@code $2a$...})
     */
    public static boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        String normalized = normalizePhpBcryptPrefix(storedHash);
        try {
            return BCrypt.checkpw(plainPassword, normalized);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String normalizePhpBcryptPrefix(String hash) {
        if (hash.startsWith("$2y$")) {
            return "$2a$" + hash.substring(4);
        }
        return hash;
    }
}
