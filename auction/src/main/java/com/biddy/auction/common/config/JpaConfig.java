package com.biddy.auction.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 공통 설정.
 *
 * <p>{@code @EnableJpaAuditing}을 활성화하여
 * {@code @CreatedDate}, {@code @LastModifiedDate} 등
 * Auditing 어노테이션이 동작하도록 한다.</p>
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
