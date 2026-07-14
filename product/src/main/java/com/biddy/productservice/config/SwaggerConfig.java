package com.biddy.productservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    @Bean
    public OpenAPI productOpenAPI() {
        SecurityScheme memberIdScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(MEMBER_ID_HEADER);

        return new OpenAPI()
                .info(new Info()
                        .title("Product Service API")
                        .description("상품 도메인 API 명세서")
                        .version("v1.0.0"))
                .components(new Components().addSecuritySchemes(MEMBER_ID_HEADER, memberIdScheme))
                .addSecurityItem(new SecurityRequirement().addList(MEMBER_ID_HEADER));
    }
}