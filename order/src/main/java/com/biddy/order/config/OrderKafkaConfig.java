package com.biddy.order.config;

import com.biddy.order.cart.application.dto.CartInfoCommand;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class OrderKafkaConfig {
    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, CartInfoCommand> CartProducerFactory() {
        // Kafka 프로듀서가 사용할 직렬화 및 연결 정보를 구성한다.
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // 프로듀서가 연결할 브로커 주소
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // 메시지 키를 문자열로 직렬화
//        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class); // 메시지 값을 JSON으로 직렬화
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class); // 메시지 값을 JSON으로 직렬화
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); // 헤더에 타입 정보를 넣지 않아도 되도록 설정
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, CartInfoCommand> orderKafkaTemplate() {
        // ProducerFactory 기반 KafkaTemplate을 생성한다.
        return new KafkaTemplate<>(CartProducerFactory());
    }
}
