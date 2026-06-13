package com.mphasis.tse.config.totp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts and decrypts TOTP secrets using AES-256-GCM.
 * Each ciphertext is prefixed with a 12-byte random IV to ensure semantic security.
 */
@Component
public class TotpCryptoHelper {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // bits

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public TotpCryptoHelper(@Value("${security.totp.encryption-key}") String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("security.totp.encryption-key must be configured. Application cannot start without it.");
        }
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] aesKey = new byte[32]; // AES-256
        System.arraycopy(keyBytes, 0, aesKey, 0, Math.min(keyBytes.length, 32));
        this.secretKey = new SecretKeySpec(aesKey, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext: [IV (12 bytes) | ciphertext + tag]
            byte[] combined = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting TOTP secret", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            // Extract IV from the first 12 bytes
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting TOTP secret", e);
        }
    }
}
