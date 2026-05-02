package org.example.totp;

import java.security.SecureRandom;

/**
 * Base32 RFC 4648 (alphabet A-Z2-7) sans padding obligatoire.
 */
public final class Base32 {
    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int[] LOOKUP = new int[128];
    private static final SecureRandom RNG = new SecureRandom();

    static {
        for (int i = 0; i < LOOKUP.length; i++) {
            LOOKUP[i] = -1;
        }
        for (int i = 0; i < ALPHABET.length; i++) {
            LOOKUP[ALPHABET[i]] = i;
        }
        for (int i = 0; i < 26; i++) {
            LOOKUP['a' + i] = i;
        }
    }

    private Base32() {
    }

    public static String randomSecret(int bytes) {
        byte[] b = new byte[Math.max(10, bytes)];
        RNG.nextBytes(b);
        return encode(b);
    }

    public static String encode(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = data[0] & 0xFF;
        int next = 1;
        int bitsLeft = 8;
        while (bitsLeft > 0 || next < data.length) {
            if (bitsLeft < 5) {
                if (next < data.length) {
                    buffer <<= 8;
                    buffer |= data[next++] & 0xFF;
                    bitsLeft += 8;
                } else {
                    int pad = 5 - bitsLeft;
                    buffer <<= pad;
                    bitsLeft += pad;
                }
            }
            int index = (buffer >> (bitsLeft - 5)) & 0x1F;
            bitsLeft -= 5;
            sb.append(ALPHABET[index]);
        }
        return sb.toString();
    }

    public static byte[] decode(String s) {
        if (s == null) {
            return new byte[0];
        }
        String str = s.trim().replace("=", "").replace(" ", "");
        if (str.isEmpty()) {
            return new byte[0];
        }
        int outLen = str.length() * 5 / 8;
        byte[] out = new byte[outLen];
        int buffer = 0;
        int bitsLeft = 0;
        int outPos = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= LOOKUP.length || LOOKUP[c] == -1) {
                throw new IllegalArgumentException("Caractère Base32 invalide: " + c);
            }
            buffer = (buffer << 5) | LOOKUP[c];
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[outPos++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
                if (outPos == out.length) {
                    break;
                }
            }
        }
        return out;
    }
}

