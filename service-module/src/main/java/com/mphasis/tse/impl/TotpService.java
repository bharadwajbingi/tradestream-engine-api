package com.mphasis.tse.impl;

import com.mphasis.tse.config.totp.TotpCryptoHelper;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TotpService {

    private final TotpCryptoHelper cryptoHelper;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), timeProvider);
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String encryptSecret(String secret) {
        return cryptoHelper.encrypt(secret);
    }

    public String decryptSecret(String encryptedSecret) {
        return cryptoHelper.decrypt(encryptedSecret);
    }

    public String getQrCodeImageBase64(String email, String secret) {
        QrData qrData = new QrData.Builder()
                .label(email)
                .issuer("TradeStreamEngine")
                .secret(secret)
                .digits(6)
                .period(30)
                .algorithm(HashingAlgorithm.SHA1)
                .build();

        try {
            byte[] qrBytes = qrGenerator.generate(qrData);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes);
        } catch (QrGenerationException e) {
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }

    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }
}
