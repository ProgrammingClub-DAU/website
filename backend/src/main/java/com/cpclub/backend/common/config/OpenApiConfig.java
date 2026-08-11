package com.cpclub.backend.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the generated OpenAPI document and shared bearer-token security scheme.
 *
 * <p>Publishing the scheme once lets protected endpoints appear consistently in Swagger
 * UI and provides clients with an accurate contract for JWT authentication.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the public OpenAPI metadata and HTTP bearer authentication definition.
     *
     * @return configured OpenAPI document exposed by springdoc
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("CP Club API Documentation")
                        .version("1.0.0")
                        .description("REST API backend for Competitive Programming Club DAU website.")
                        .contact(new Contact().name("CP Club Tech Team").email("cpclub@example.com")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
