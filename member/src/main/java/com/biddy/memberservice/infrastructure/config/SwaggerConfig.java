package com.biddy.memberservice.infrastructure.config;

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
 * <p>Member Service는 실제 운영 환경에서 API Gateway가 JWT를 검증한 뒤
 * X-Member-Id / X-Member-Role 헤더로 인증 정보를 전달받는다
 * ({@code HeaderAuthenticationFilter} 참고). Gateway를 거치지 않고
 * Swagger UI에서 직접 호출할 때는 이 헤더를 수동으로 채워야 인증된 것처럼
 * 테스트할 수 있다.</p>
 *
 * <p>Swagger UI 우측 상단 "Authorize" 버튼을 누르면 X-Member-Id, X-Member-Role
 * 값을 입력할 수 있고, 입력 후에는 모든 "Try it out" 요청에 자동으로 포함된다.</p>
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String memberIdScheme = "X-Member-Id";
        String memberRoleScheme = "X-Member-Role";

        return new OpenAPI()
                .info(new Info()
                        .title("Member Service API")
                        .description("Biddy 회원 서비스 API")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement()
                        .addList(memberIdScheme)
                        .addList(memberRoleScheme))
                .components(new Components()
                        .addSecuritySchemes(memberIdScheme,
                                new SecurityScheme()
                                        .name(memberIdScheme)
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("테스트할 회원 ID (예: 1)"))
                        .addSecuritySchemes(memberRoleScheme,
                                new SecurityScheme()
                                        .name(memberRoleScheme)
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("테스트할 회원 권한 (예: USER, ADMIN)")));
    }
}
