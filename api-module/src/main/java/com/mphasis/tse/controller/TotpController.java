package com.mphasis.tse.controller;

import com.mphasis.tse.dto.ApiResponse;
import com.mphasis.tse.entity.User;
import com.mphasis.tse.filter.ExportTokenService;
import com.mphasis.tse.impl.TotpService;
import com.mphasis.tse.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth/totp")
@RequiredArgsConstructor
public class TotpController {

    private final TotpService totpService;
    private final UserRepository userRepository;
    private final ExportTokenService exportTokenService;

    @Data
    public static class TotpCodeRequest {
        private String code;
    }

    @Data
    public static class TotpSetupResponse {
        private String qrCodeUrl;
        private String secret;

        public TotpSetupResponse(String qrCodeUrl, String secret) {
            this.qrCodeUrl = qrCodeUrl;
            this.secret = secret;
        }
    }

    @Data
    public static class TotpStatusResponse {
        private boolean enabled;

        public TotpStatusResponse(boolean enabled) {
            this.enabled = enabled;
        }
    }

    @Operation(summary = "Generate TOTP secret and QR code for 2FA setup")
    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<TotpSetupResponse>> setupTotp(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isTotpEnabled()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "BAD_REQUEST", 400, "TOTP is already enabled for this account.", null
            ));
        }

        String secret = totpService.generateSecret();
        String encrypted = totpService.encryptSecret(secret);
        user.setTotpSecret(encrypted);
        userRepository.save(user);

        String qrCodeUrl = totpService.getQrCodeImageBase64(user.getEmail(), secret);
        TotpSetupResponse response = new TotpSetupResponse(qrCodeUrl, secret);

        return ResponseEntity.ok(new ApiResponse<>(
                "OK", 200, "TOTP secret generated successfully. Scan the QR code to proceed.", response
        ));
    }

    @Operation(summary = "Enable TOTP 2FA by verifying the first code")
    @PostMapping("/enable")
    public ResponseEntity<ApiResponse<Void>> enableTotp(@AuthenticationPrincipal User principal, @RequestBody TotpCodeRequest req) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTotpSecret() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "BAD_REQUEST", 400, "TOTP setup has not been initiated.", null
            ));
        }

        String plainSecret = totpService.decryptSecret(user.getTotpSecret());
        boolean isValid = totpService.verifyCode(plainSecret, req.getCode());

        if (!isValid) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "BAD_REQUEST", 400, "Invalid verification code. Please try again.", null
            ));
        }

        user.setTotpEnabled(true);
        userRepository.save(user);

        return ResponseEntity.ok(new ApiResponse<>(
                "OK", 200, "TOTP 2FA has been successfully enabled on your account.", null
        ));
    }

    @Operation(summary = "Verify TOTP code to retrieve a 5-minute export token")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyTotp(@AuthenticationPrincipal User principal, @RequestBody TotpCodeRequest req) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isTotpEnabled() || user.getTotpSecret() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "BAD_REQUEST", 400, "TOTP is not enabled for this account.", null
            ));
        }

        String plainSecret = totpService.decryptSecret(user.getTotpSecret());
        boolean isValid = totpService.verifyCode(plainSecret, req.getCode());

        if (!isValid) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "BAD_REQUEST", 400, "Invalid verification code.", null
            ));
        }

        String exportToken = exportTokenService.generateExportToken(user.getEmail());

        return ResponseEntity.ok(new ApiResponse<>(
                "OK", 200, "TOTP code verified. Use the retrieved token for exports.", exportToken
        ));
    }

    @Operation(summary = "Check the TOTP enablement status for the current user")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<TotpStatusResponse>> getTotpStatus(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TotpStatusResponse response = new TotpStatusResponse(user.isTotpEnabled());
        return ResponseEntity.ok(new ApiResponse<>(
                "OK", 200, "TOTP status retrieved.", response
        ));
    }

    @Operation(summary = "Disable TOTP 2FA by verifying a final code")
    @DeleteMapping("/disable")
    public ResponseEntity<ApiResponse<Void>> disableTotp(@AuthenticationPrincipal User principal, @RequestParam String code) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isTotpEnabled() || user.getTotpSecret() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "BAD_REQUEST", 400, "TOTP is not enabled for this account.", null
            ));
        }

        String plainSecret = totpService.decryptSecret(user.getTotpSecret());
        boolean isValid = totpService.verifyCode(plainSecret, code);

        if (!isValid) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "BAD_REQUEST", 400, "Invalid verification code. Disable failed.", null
            ));
        }

        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);

        return ResponseEntity.ok(new ApiResponse<>(
                "OK", 200, "TOTP 2FA has been disabled on your account.", null
        ));
    }
}
