package com.biddy.payment.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger (OpenAPI) 설정.
 *
 * <p>JWT Bearer 인증을 Swagger UI에서 사용할 수 있도록 설정한다.
 * Member Service에서 로그인 후 받은 accessToken을 Authorize에 입력하면
 * 모든 API 요청에 Authorization 헤더가 자동 포함된다.</p>
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .description("Biddy 결제 서비스 API")
                        .version("2.0"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Member Service 로그인 후 받은 accessToken을 입력하세요")));
    }
}
