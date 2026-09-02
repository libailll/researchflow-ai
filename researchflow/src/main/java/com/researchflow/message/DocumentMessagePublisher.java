package com.researchflow.message;

import com.researchflow.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishParse(DocumentParseMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.DOCUMENT_EXCHANGE,
                RabbitMqConfig.DOCUMENT_PARSE_ROUTING_KEY,
                message
        );
    }

    public void publishVectorize(DocumentVectorMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.DOCUMENT_EXCHANGE,
                RabbitMqConfig.DOCUMENT_VECTORIZE_ROUTING_KEY,
                message
        );
    }

    public void publishVectorDelete(DocumentVectorMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.DOCUMENT_EXCHANGE,
                RabbitMqConfig.DOCUMENT_VECTOR_DELETE_ROUTING_KEY,
                message
        );
    }
}
