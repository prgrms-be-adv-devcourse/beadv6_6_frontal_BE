package com.biddy.recommendation.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

// [2026-07-14] 예전 코드(DataSource 레벨에 @Primary를 안 붙이고 JdbcTemplate에만 @Primary를 붙였던 버전):
//
// @Bean
// @ConfigurationProperties(prefix = "product-db")
// public DataSource productDataSource() {
//     return DataSourceBuilder.create().build();
// }
//
// @Bean
// @Primary
// public JdbcTemplate jdbcTemplate(DataSource dataSource) {
//     return new JdbcTemplate(dataSource);
// }
//
// @Bean
// public JdbcTemplate productJdbcTemplate(@Qualifier("productDataSource") DataSource productDataSource) {
//     return new JdbcTemplate(productDataSource);
// }
//
// 문제: DataSource 빈이 2개(자동설정된 것 + productDataSource)인데 DataSource 레벨에는 @Primary가
// 없어서, schema.sql을 실행하는 DataSourceScriptDatabaseInitializer 같은 내부 컴포넌트가
// 어느 DataSource를 써야 할지 애매해졌고, 실제로는 productDataSource(biddy_product) 쪽으로 붙어버려서
// user_interest/match_notification 테이블이 엉뚱하게 biddy_product에 생성됨.
//
// [2026-07-14, 2차 시도] DataSourceProperties로 primary DataSource를 재구성해서 @Primary를 붙였던 버전.
// 빈 이름을 "dataSource"/"jdbcTemplate"로 지었었는데, 이건 Spring Boot가 자동 설정할 때 쓰는 이름과
// 완전히 같음 — @ConditionalOnMissingBean이 막아줄 거라 가정했지만 실제로 검증은 안 했고, 진단 로그로
// 확인해보니 @Qualifier가 아예 없는 UserInterestRepositoryAdapter조차 엉뚱한 DB(biddy_product)로
// 연결되는 현상이 있었음(원인 미확정). 같은 실수를 반복 안 하려고, 이번엔 Spring Boot 관례와
// 절대 안 겹치는 이름으로 바꾸고, 진단 로그를 남겨서 실제로 검증하기 전까지는 "고쳤다"고 안 함.
@Slf4j
@Configuration
public class ProductDbConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties recommendationDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource recommendationDataSource(DataSourceProperties recommendationDataSourceProperties) {
        DataSource dataSource = recommendationDataSourceProperties.initializeDataSourceBuilder().build();
        if (dataSource instanceof HikariDataSource hikari) {
            log.info(">>> [진단] recommendationDataSource jdbcUrl = {}", hikari.getJdbcUrl());
        }
        return dataSource;
    }

    @Bean
    @ConfigurationProperties(prefix = "product-db")
    public DataSource productDataSource() {
        DataSource dataSource = DataSourceBuilder.create().build();
        if (dataSource instanceof HikariDataSource hikari) {
            log.info(">>> [진단] productDataSource jdbcUrl = {}", hikari.getJdbcUrl());
        }
        return dataSource;
    }

    @Primary
    @Bean
    public JdbcTemplate recommendationJdbcTemplate(@Qualifier("recommendationDataSource") DataSource recommendationDataSource) {
        return new JdbcTemplate(recommendationDataSource);
    }

    @Bean
    public JdbcTemplate productJdbcTemplate(@Qualifier("productDataSource") DataSource productDataSource) {
        return new JdbcTemplate(productDataSource);
    }
}
