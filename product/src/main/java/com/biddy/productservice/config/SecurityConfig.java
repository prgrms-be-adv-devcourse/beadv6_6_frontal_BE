package com.biddy.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 이미지 파일 및 헬스 체크는 누구나 접근 가능
                        .requestMatchers(HttpMethod.GET, "/images/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // 스웨거 문서는 누구나 접근 가능 (다른 서비스들과 동일하게)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 로그인 필요한 GET (내 찜 목록)
                        .requestMatchers(HttpMethod.GET, "/api/products/liked").authenticated()
                        // 비로그인도 가능한 상품 조회
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                        // recommendation-service가 임베딩 저장을 위임하는 내부 전용 API.
                        // 게이트웨이 라우트가 없어 외부에서 직접 호출은 안 되지만, 별도 서비스 간 인증은 아직 없음 —
                        // 나중에 내부망 제한(NetworkPolicy)이나 서비스 간 시크릿 검증으로 보강 필요.
                        .requestMatchers(HttpMethod.PUT, "/api/products/*/embedding").permitAll()
                        // 나머지 (등록/수정/삭제/이미지업로드/찜하기 등)는 인증 필요
                        .anyRequest().authenticated())
                .addFilterBefore(new HeaderAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://biddy-zeta.vercel.app",
                "https://*.vercel.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
