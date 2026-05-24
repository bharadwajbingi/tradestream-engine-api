package com.mphasis.tse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {

                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://localhost:8080", 
                                "http://localhost:8081", 
                                "http://localhost:8082", 
                                "http://localhost:3000", 
                                "http://localhost:4200", 
                                "http://localhost:5173", 
                                "http://localhost:5174",
                                "https://d2i9y8go17l95q.cloudfront.net"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}