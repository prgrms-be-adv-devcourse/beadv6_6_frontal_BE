package com.biddy.recommendation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI recommendationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Recommendation Service API")
                        .description("추천 도메인 API 명세서")
                        .version("v1.0.0"));
    }
}
