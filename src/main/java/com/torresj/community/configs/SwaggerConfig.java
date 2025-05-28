package com.torresj.community.configs;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer")
public class SwaggerConfig {

    @Value("${info.app.version}")
    private final String version;

    @Bean
    public OpenAPI springOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Community API")
                                .description("Community API")
                                .version(version)
                                .license(
                                        new License()
                                                .name("GNU General Public License V3.0")
                                                .url("https://www.gnu.org/licenses/gpl-3.0.html")));
    }
}