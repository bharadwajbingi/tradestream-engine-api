package com.mphasis.tse.config.totp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AES-256-GCM encryption used to protect TOTP secrets in the database.
 *
 * Key behaviours verified:
 *  - Encrypt → Decrypt round-trip produces the original plaintext
 *  - Two encryptions of the same value produce DIFFERENT ciphertexts (random IV)
 *  - Null inputs are handled gracefully
 *  - Tampered ciphertext is rejected (GCM authentication tag verification)
 *  - Missing encryption key causes fast-fail at construction time
 */
@DisplayName("TotpCryptoHelper — AES-256-GCM encryption")
class TotpCryptoHelperTest {

    // 32-char key satisfies the AES-256 requirement
    private static final String TEST_KEY = "test-encryption-key-32-chars-ok!";

    private TotpCryptoHelper cryptoHelper;

    @BeforeEach
    void setUp() {
        cryptoHelper = new TotpCryptoHelper(TEST_KEY);
    }

    // -----------------------------------------------------------------------
    // Happy-path: round-trip correctness
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("encrypt then decrypt returns original plaintext")
    void encryptDecrypt_roundTrip_returnsOriginal() {
        String original = "JBSWY3DPEHPK3PXP"; // typical TOTP secret (Base32)

        String cipherText = cryptoHelper.encrypt(original);
        String decrypted = cryptoHelper.decrypt(cipherText);

        assertEquals(original, decrypted);
    }

    @Test
    @DisplayName("encrypt then decrypt works for long secrets")
    void encryptDecrypt_longSecret_roundTrip() {
        String longSecret = "A".repeat(256);

        String cipherText = cryptoHelper.encrypt(longSecret);
        String decrypted = cryptoHelper.decrypt(cipherText);

        assertEquals(longSecret, decrypted);
    }

    @Test
    @DisplayName("encrypt then decrypt works for special characters")
    void encryptDecrypt_specialCharacters_roundTrip() {
        String specialChars = "!@#$%^&*()_+{}|:<>?`~";

        String cipherText = cryptoHelper.encrypt(specialChars);
        String decrypted = cryptoHelper.decrypt(cipherText);

        assertEquals(specialChars, decrypted);
    }

    // -----------------------------------------------------------------------
    // Semantic security: same input → different ciphertext each time (random IV)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("two encryptions of same value produce different ciphertexts (random IV)")
    void encrypt_sameInput_producesDistinctCiphertexts() {
        String secret = "JBSWY3DPEHPK3PXP";

        String cipher1 = cryptoHelper.encrypt(secret);
        String cipher2 = cryptoHelper.encrypt(secret);

        // If IVs are truly random, ciphertexts must differ
        assertNotEquals(cipher1, cipher2,
                "Two encryptions of the same plaintext must yield different ciphertexts (random IV ensures semantic security)");
    }

    // -----------------------------------------------------------------------
    // Null / empty handling
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("encrypt(null) returns null without throwing")
    void encrypt_null_returnsNull() {
        assertNull(cryptoHelper.encrypt(null));
    }

    @Test
    @DisplayName("decrypt(null) returns null without throwing")
    void decrypt_null_returnsNull() {
        assertNull(cryptoHelper.decrypt(null));
    }

    // -----------------------------------------------------------------------
    // Tamper detection: GCM authentication tag must reject modified ciphertext
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("tampered ciphertext is rejected by GCM authentication tag")
    void decrypt_tamperedCiphertext_throwsException() {
        String original = "JBSWY3DPEHPK3PXP";
        String cipherText = cryptoHelper.encrypt(original);

        // Flip the last character of the Base64 string to simulate tampering
        String tampered = cipherText.substring(0, cipherText.length() - 1) + "X";

        // GCM authentication should detect the tampering
        assertThrows(RuntimeException.class, () -> cryptoHelper.decrypt(tampered),
                "Tampered ciphertext must throw — GCM tag verification should fail");
    }

    @Test
    @DisplayName("completely invalid Base64 ciphertext throws RuntimeException")
    void decrypt_invalidBase64_throwsException() {
        assertThrows(RuntimeException.class,
                () -> cryptoHelper.decrypt("not-valid-base64!!!"),
                "Invalid input must throw RuntimeException");
    }

    // -----------------------------------------------------------------------
    // Fail-fast: missing key must prevent startup
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("blank encryption key throws IllegalStateException at construction")
    void constructor_blankKey_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> new TotpCryptoHelper(""),
                "Blank key must throw IllegalStateException — app should not start without a key");
    }

    @Test
    @DisplayName("null encryption key throws IllegalStateException at construction")
    void constructor_nullKey_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> new TotpCryptoHelper(null),
                "Null key must throw IllegalStateException");
    }

    // -----------------------------------------------------------------------
    // Ciphertext format: must be valid Base64
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("ciphertext is valid Base64 encoded string")
    void encrypt_output_isValidBase64() {
        String cipherText = cryptoHelper.encrypt("JBSWY3DPEHPK3PXP");

        // Should not throw — valid Base64
        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(cipherText));
    }

    @Test
    @DisplayName("ciphertext is longer than plaintext (IV + tag overhead)")
    void encrypt_output_isLongerThanPlaintext() {
        String plainText = "JBSWY3DPEHPK3PXP";
        String cipherText = cryptoHelper.encrypt(plainText);

        // AES-GCM adds 12-byte IV + 16-byte tag = 28 bytes overhead minimum
        assertTrue(cipherText.length() > plainText.length(),
                "Ciphertext must be longer than plaintext due to IV and GCM tag");
    }
}
