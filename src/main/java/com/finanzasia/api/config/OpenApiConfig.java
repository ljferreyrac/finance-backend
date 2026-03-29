package com.finanzasia.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finanzas IA API")
                        .description("""
                                AI-powered personal finance API for the Peruvian market.
                                Supports expense tracking with dual-currency (PEN/USD),
                                SUNAT receipt scanning, and Peruvian bank card management.

                                **Authentication**: All endpoints require a JWT access token.
                                Click **Authorize** and enter: `Bearer <your_token>`
                                """)
                        .version("v1")
                        .contact(new Contact()
                                .name("Finanzas IA")
                                .email("dev@finanzasia.com")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT access token (without the 'Bearer ' prefix).")));
    }
}
