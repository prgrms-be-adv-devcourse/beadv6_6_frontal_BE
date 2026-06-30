package com.biddy.auction.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Gateway 헤더 또는 Bearer 토큰 기반 인증 필터.
 *
 * <p>Gateway가 JWT를 검증한 뒤 전달한 X-Member-Id/X-Member-Role 헤더를 우선 사용한다.
 * Gateway를 거치지 않은 직접 호출은 기존처럼 Bearer 토큰을 검증해야 인증된다.</p>
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public HeaderAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String gatewayMemberId = request.getHeader("X-Member-Id");
        String gatewayRole = request.getHeader("X-Member-Role");
        String authorization = request.getHeader("Authorization");

        if (gatewayMemberId != null && gatewayRole != null && authorization != null) {
            setAuthentication(Long.valueOf(gatewayMemberId), gatewayRole);
        } else if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                Long memberId = jwtTokenProvider.getMemberId(token);
                String role = jwtTokenProvider.getRole(token);
                setAuthentication(memberId, role);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(Long memberId, String role) {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER"))
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
