package com.conceptclarity.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final String PREFIX = "pbkdf2$";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    public String hashPassword(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    HexFormat.of().parseHex(salt),
                    ITERATIONS,
                    KEY_LENGTH_BITS
            );
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Password hashing is not available.", ex);
        }
    }

    public boolean matches(String rawPassword, String salt, String expectedHash) {
        String submittedHash = hashPassword(rawPassword, salt);
        return MessageDigest.isEqual(
                submittedHash.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    public boolean matchesLegacySha256(String rawPassword, String salt, String expectedHash) {
        if (expectedHash != null && expectedHash.startsWith(PREFIX)) {
            return false;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((salt + rawPassword).getBytes(StandardCharsets.UTF_8));
            String submittedHash = HexFormat.of().formatHex(hashed);
            return MessageDigest.isEqual(
                    submittedHash.getBytes(StandardCharsets.UTF_8),
                    expectedHash.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Legacy password hashing is not available.", ex);
        }
    }
}
