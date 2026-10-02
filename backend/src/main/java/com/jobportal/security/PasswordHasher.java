package com.jobportal.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Salted PBKDF2-HMAC-SHA256 password hashing using only the JDK.
 *
 * Stored format: {@code iterations:base64(salt):base64(hash)} so the iteration count can be raised
 * later without breaking existing hashes. Phase 4 swaps this for Spring Security's BCryptPasswordEncoder.
 *
 * Never store or log raw passwords, and never compare hashes with equals(): use a constant-time
 * comparison so response timing doesn't leak how many bytes matched.
 */
public class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final SecureRandom random = new SecureRandom();
    private final int iterations;

    public PasswordHasher(int iterations) {
        if (iterations < 1) throw new IllegalArgumentException("iterations must be positive");
        this.iterations = iterations;
    }

    public String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(rawPassword.toCharArray(), salt, iterations);
        Base64.Encoder b64 = Base64.getEncoder();
        return iterations + ":" + b64.encodeToString(salt) + ":" + b64.encodeToString(hash);
    }

    public boolean matches(String rawPassword, String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 3) return false;
        int storedIterations = Integer.parseInt(parts[0]);
        Base64.Decoder b64 = Base64.getDecoder();
        byte[] salt = b64.decode(parts[1]);
        byte[] expected = b64.decode(parts[2]);
        byte[] actual = derive(rawPassword.toCharArray(), salt, storedIterations);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        } finally {
            spec.clearPassword();
        }
    }
}
