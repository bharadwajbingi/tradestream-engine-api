package com.mphasis.tse.impl;

import com.mphasis.tse.config.totp.TotpCryptoHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TotpService — TOTP secret management and verification")
class TotpServiceTest {

    @Mock
    private TotpCryptoHelper cryptoHelper;

    @InjectMocks
    private TotpService totpService;

    @Test
    @DisplayName("generateSecret returns a non-null non-empty string")
    void generateSecret_returnsValue() {
        String secret = totpService.generateSecret();
        assertNotNull(secret);
        assertFalse(secret.isBlank());
    }

    @Test
    @DisplayName("encryptSecret delegates to TotpCryptoHelper.encrypt")
    void encryptSecret_delegatesToHelper() {
        when(cryptoHelper.encrypt("MY_SECRET")).thenReturn("ENCRYPTED");

        String result = totpService.encryptSecret("MY_SECRET");

        assertEquals("ENCRYPTED", result);
        verify(cryptoHelper).encrypt("MY_SECRET");
    }

    @Test
    @DisplayName("decryptSecret delegates to TotpCryptoHelper.decrypt")
    void decryptSecret_delegatesToHelper() {
        when(cryptoHelper.decrypt("ENCRYPTED")).thenReturn("MY_SECRET");

        String result = totpService.decryptSecret("ENCRYPTED");

        assertEquals("MY_SECRET", result);
        verify(cryptoHelper).decrypt("ENCRYPTED");
    }

    @Test
    @DisplayName("getQrCodeImageBase64 returns data URI with base64 PNG")
    void getQrCodeImageBase64_returnsDataUri() {
        String qr = totpService.getQrCodeImageBase64("user@example.com", "JBSWY3DPEHPK3PXP");

        assertNotNull(qr);
        assertTrue(qr.startsWith("data:image/png;base64,"));
        assertTrue(qr.length() > 100, "QR image should have substantial base64 content");
    }

    @Test
    @DisplayName("verifyCode returns false for wrong code")
    void verifyCode_wrongCode_returnsFalse() {
        boolean result = totpService.verifyCode("JBSWY3DPEHPK3PXP", "000000");
        // The code is almost certainly wrong (1 in a million chance it's right)
        // This tests that the method runs without errors
        assertFalse(result);
    }
}
