package com.biddy.order.order.infra.event;

import com.biddy.order.order.application.usecase.OrderUseCase;
import com.biddy.order.order.domain.model.OrderStatus;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"payment.completed", "payment.failed"})
@TestPropertySource(properties = {
        "kafka.enabled=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
public class OrderPaymentEventListenerTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockitoBean
    private OrderUseCase orderUseCase;

    @MockitoBean
    private org.springframework.kafka.core.KafkaTemplate<String, String> orderKafkaTemplate;

    @Test
    public void testPaymentCompletedEventConsumption() throws Exception {
        // Given
        String payload = "{\"paymentId\":1,\"orderId\":10,\"buyerId\":100,\"sellerId\":200,\"amount\":5000,\"paymentMethod\":\"CARD\",\"occurredAt\":\"2026-06-21T23:48:05\"}";

        // When
        sendTestMessage("payment.completed", "10", payload);

        // Then
        verify(orderUseCase, timeout(5000)).changeStatus(eq(100L), eq(10L), eq(OrderStatus.PAID));
    }

    @Test
    public void testPaymentFailedEventConsumption() throws Exception {
        // Given
        String payload = "{\"paymentId\":2,\"orderId\":20,\"buyerId\":100,\"amount\":5000,\"reason\":\"No funds\",\"occurredAt\":\"2026-06-21T23:48:05\"}";

        // When
        sendTestMessage("payment.failed", "20", payload);

        // Then
        verify(orderUseCase, timeout(5000)).changeStatus(eq(100L), eq(20L), eq(OrderStatus.CANCELLED));
    }

    private void sendTestMessage(String topic, String key, String payload) {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(configs);
        KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.send(topic, key, payload);
    }
}
