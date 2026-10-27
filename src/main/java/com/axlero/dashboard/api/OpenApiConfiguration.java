package com.axlero.dashboard.api;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "sessionCookie",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "JSESSIONID",
        description = "Authenticated session cookie returned by /api/auth/login")
public class OpenApiConfiguration {

    @Bean
    public OpenAPI dashboardOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Axlero Dynamic Dashboard API")
                        .version("1.0.0")
                        .description("Session-authenticated API for dashboard and widget configuration."));
    }
}
