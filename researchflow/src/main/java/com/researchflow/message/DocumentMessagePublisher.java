package com.researchflow.message;

import com.researchflow.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DocumentMessagePublisher {

    private static final long PUBLISH_CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;

    public void publishParse(DocumentParseMessage message) {
        publishConfirmed(RabbitMqConfig.DOCUMENT_PARSE_ROUTING_KEY, message);
    }

    public void publishVectorize(DocumentVectorMessage message) {
        publishConfirmed(RabbitMqConfig.DOCUMENT_VECTORIZE_ROUTING_KEY, message);
    }

    public void publishVectorDelete(DocumentVectorMessage message) {
        publishConfirmed(RabbitMqConfig.DOCUMENT_VECTOR_DELETE_ROUTING_KEY, message);
    }

    private void publishConfirmed(String routingKey, Object payload) {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.DOCUMENT_EXCHANGE,
                routingKey,
                payload,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setMessageId(correlationData.getId());
                    return message;
                },
                correlationData
        );
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(PUBLISH_CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ rejected message: " + confirm.getReason());
            }
            if (correlationData.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ message was unroutable: " + routingKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for RabbitMQ publish confirmation", e);
        } catch (Exception e) {
            throw new IllegalStateException("RabbitMQ publish confirmation failed", e);
        }
    }
}
