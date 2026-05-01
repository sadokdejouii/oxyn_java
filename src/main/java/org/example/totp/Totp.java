package org.example.totp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * TOTP RFC 6238 (HMAC-SHA1, 30s, 6 digits).
 */
public final class Totp {

    private Totp() {
    }

    public static String buildOtpAuthUri(String issuer, String accountName, String secretBase32) {
        String iss = issuer != null ? issuer : "OXYN";
        String acc = accountName != null ? accountName : "user";
        String label = urlEncode(iss + ":" + acc);
        return "otpauth://totp/" + label +
                "?secret=" + urlEncode(secretBase32) +
                "&issuer=" + urlEncode(iss) +
                "&algorithm=SHA1&digits=6&period=30";
    }

    public static boolean verifyCode(String secretBase32, String code, long nowMillis) {
        if (secretBase32 == null || secretBase32.isBlank() || code == null) {
            return false;
        }
        String normalized = code.trim().replace(" ", "");
        if (!normalized.matches("^\\d{6}$")) {
            return false;
        }
        int expected = Integer.parseInt(normalized);

        // window ±1 step
        long step = (nowMillis / 1000L) / 30L;
        for (long w = -1; w <= 1; w++) {
            int gen = generate(secretBase32, step + w);
            if (gen == expected) {
                return true;
            }
        }
        return false;
    }

    public static int generate(String secretBase32, long timeStep) {
        try {
            byte[] key = Base32.decode(secretBase32);
            ByteBuffer bb = ByteBuffer.allocate(8);
            bb.putLong(timeStep);
            byte[] msg = bb.array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(msg);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary =
                    ((hash[offset] & 0x7F) << 24) |
                            ((hash[offset + 1] & 0xFF) << 16) |
                            ((hash[offset + 2] & 0xFF) << 8) |
                            (hash[offset + 3] & 0xFF);
            return binary % 1_000_000;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de générer TOTP: " + e.getMessage(), e);
        }
    }

    public static String format6(int code) {
        return String.format(java.util.Locale.ROOT, "%06d", code);
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

