package com.biddy.order.config;

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
 * order-service 전용 헤더 기반 인증 필터.
 * X-Member-Id 헤더가 존재하면 인증 객체를 생성하며,
 * X-Member-Role 헤더가 없을 경우 기본값으로 "USER" 역할을 부여한다.
 */
public class OrderHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String memberId = request.getHeader("X-Member-Id");
            String role = request.getHeader("X-Member-Role");

            if (memberId != null && !memberId.trim().isEmpty() && !"null".equalsIgnoreCase(memberId) && !"undefined".equalsIgnoreCase(memberId)) {
                if (role == null || role.trim().isEmpty() || "null".equalsIgnoreCase(role) || "undefined".equalsIgnoreCase(role)) {
                    role = "USER";
                }
                
                String authorityName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                Long.valueOf(memberId),
                                null,
                                List.of(new SimpleGrantedAuthority(authorityName))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.info("Successfully authenticated request for memberId: " + memberId + " with role: " + authorityName);
            }
        } catch (Exception e) {
            logger.error("Failed to parse authentication headers in OrderHeaderAuthenticationFilter: " + e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
