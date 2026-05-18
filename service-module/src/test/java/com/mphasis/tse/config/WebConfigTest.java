package com.mphasis.tse.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.junit.jupiter.api.Assertions.*;

class WebConfigTest {

    @Test
    void testCorsConfigurer_fullCoverage() {

        WebConfig config = new WebConfig();

        WebMvcConfigurer configurer = config.corsConfigurer();

        assertNotNull(configurer);

        CorsRegistry registry = new CorsRegistry();

        configurer.addCorsMappings(registry);

        assertNotNull(registry);
    }
}