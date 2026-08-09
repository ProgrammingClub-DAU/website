package com.cpclub.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cpClubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CP Club API")
                        .description("Competitive Programming Club Website REST API")
                        .version("1.0.0"));
    }
}
