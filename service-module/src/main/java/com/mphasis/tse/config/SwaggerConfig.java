package com.mphasis.tse.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String description = """
                # Trade Stream Processing Engine (TSE) - API Portal
                
                Welcome to the official developer sandbox and documentation portal for the Trade Stream Processing Engine. 
                TSE is an enterprise-grade, high-throughput, and highly available asynchronous trade ingestion and validation system.
                
                ---
                
                ### 1. System Design & Asynchronous Architecture
                TSE completely decouples file upload requests from heavy processing tasks:
                * **Instant Ingestion:** Files up to **1GB** are uploaded via `POST /file/upload`. The servlet thread writes the file directly to persistent disk, creates a `PENDING` database record, and returns an HTTP `202 Accepted` response instantly (under 1 second).
                * **Asynchronous Queue:** A background scheduler polls for queued files every 5 seconds. It strictly processes one file per user concurrently, running Spring Batch chunk-based executions (chunk size `1000`).
                * **Background Row Counting:** To eliminate upload latency, file row counting runs fully asynchronously in the background batch thread right before step execution starts.
                * **Auto-Recovery & Resume:** Interrupted jobs from crashes or container restarts are automatically recovered on boot and resume from the last committed chunk of 1000 records.
                
                ---
                
                ### 2. Authentication & Secure Data Exports
                The system enforces bulletproof, ownership-based access controls:
                * **Stateless Authentication:** Core operations require standard `Bearer <JWT_TOKEN>` headers.
                * **Google OAuth2 Login:** Primary auth flow begins at `/oauth2/authorization/google` and redirects on success to the frontend with the JWT.
                * **Dual-Factor Export Token (TOTP 2FA):** Any request attempting to export sensitive financial data (e.g. `GET /transactions/export`) must pass a transient **5-minute Export Token** in the `X-Export-Token` header (or as a `token` query param) if TOTP 2FA is active on the user's profile. You must verify a 6-digit TOTP code at `POST /auth/totp/verify` to retrieve this export token first.
                
                ---
                
                ### 3. Operational Error Handling
                All APIs return an envelope payload mapping a standard success/error structure:
                * **status** (String): Status code string (e.g. `"OK"`, `"ACCEPTED"`, `"FORBIDDEN"`).
                * **statusCode** (int): HTTP code (e.g. `200`, `202`, `403`).
                * **message** (String): Clear business message.
                * **data** (Object): Generic response payload body.
                """;

        return new OpenAPI()
                .info(new Info()
                        .title("Trade Stream Processing Engine (TSE) API")
                        .description(description)
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server (API)")
                ))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication")
                        .addList("Export Token Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization"))
                        .addSecuritySchemes("Export Token Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-Export-Token")
                                        .description("Transient 5-minute export token, required for streamed file exports if TOTP 2FA is enabled.")));
    }
}
