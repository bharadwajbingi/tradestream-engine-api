package com.mphasis.tse.controller;

import com.mphasis.tse.dto.ApiResponse;
import com.mphasis.tse.dto.ProfileResponse;
import com.mphasis.tse.entity.User;
import com.mphasis.tse.filter.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Data
    public static class UpdateNameRequest {
        private String name;
    }

    @Operation(summary = "Get the authenticated user's profile details")
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(new ApiResponse<>(
                    "UNAUTHORIZED", 401, "User is not authenticated.", null
            ));
        }

        ProfileResponse profile = userService.getProfile(principal.getEmail());
        return ResponseEntity.ok(new ApiResponse<>(
                "OK", 200, "Profile details retrieved successfully.", profile
        ));
    }

    @Operation(summary = "Update the authenticated user's name")
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateProfileName(@AuthenticationPrincipal User principal, @RequestBody UpdateNameRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(new ApiResponse<>(
                    "UNAUTHORIZED", 401, "User is not authenticated.", null
            ));
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "BAD_REQUEST", 400, "Name cannot be empty.", null
            ));
        }

        userService.updateProfileName(principal.getEmail(), request.getName().trim());
        return ResponseEntity.ok(new ApiResponse<>(
                "OK", 200, "Profile name updated successfully.", null
        ));
    }
}
