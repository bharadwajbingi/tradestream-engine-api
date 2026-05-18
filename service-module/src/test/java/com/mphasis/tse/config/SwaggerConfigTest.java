package com.mphasis.tse.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwaggerConfigTest {

    @Test
    void testOpenAPIBean() {
        SwaggerConfig config = new SwaggerConfig();

        OpenAPI openAPI = config.openAPI();

        assertNotNull(openAPI);

        Info info = openAPI.getInfo();
        assertNotNull(info);
        assertEquals("Trade File Processing API", info.getTitle());
        assertEquals("API documentation", info.getDescription());
        assertEquals("1.0.0", info.getVersion());

        assertNotNull(openAPI.getSecurity());
        assertFalse(openAPI.getSecurity().isEmpty());
        assertTrue(openAPI.getSecurity()
                .get(0)
                .containsKey("Bearer Authentication"));


        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());

        SecurityScheme scheme = openAPI.getComponents()
                .getSecuritySchemes()
                .get("Bearer Authentication");

        assertNotNull(scheme);


        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
        assertEquals(SecurityScheme.In.HEADER, scheme.getIn());
        assertEquals("Authorization", scheme.getName());
    }
}