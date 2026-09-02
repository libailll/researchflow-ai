package com.researchflow.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitMqConfig {

    public static final String DOCUMENT_EXCHANGE = "researchflow.document";
    public static final String DOCUMENT_PARSE_QUEUE = "document.parse";
    public static final String DOCUMENT_PARSE_ROUTING_KEY = "document.parse";
    public static final String DOCUMENT_VECTORIZE_QUEUE = "document.vectorize";
    public static final String DOCUMENT_VECTORIZE_ROUTING_KEY = "document.vectorize";
    public static final String DOCUMENT_VECTOR_DELETE_QUEUE = "document.vector.delete";
    public static final String DOCUMENT_VECTOR_DELETE_ROUTING_KEY = "document.vector.delete";

    @Bean
    public DirectExchange documentExchange() {
        return new DirectExchange(DOCUMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue documentParseQueue() {
        return new Queue(DOCUMENT_PARSE_QUEUE, true);
    }

    @Bean
    public Binding documentParseBinding(
            @Qualifier("documentParseQueue") Queue documentParseQueue,
            DirectExchange documentExchange
    ) {
        return BindingBuilder.bind(documentParseQueue)
                .to(documentExchange)
                .with(DOCUMENT_PARSE_ROUTING_KEY);
    }

    @Bean
    public Queue documentVectorizeQueue() {
        return new Queue(DOCUMENT_VECTORIZE_QUEUE, true);
    }

    @Bean
    public Binding documentVectorizeBinding(
            @Qualifier("documentVectorizeQueue") Queue queue,
            DirectExchange documentExchange
    ) {
        return BindingBuilder.bind(queue).to(documentExchange).with(DOCUMENT_VECTORIZE_ROUTING_KEY);
    }

    @Bean
    public Queue documentVectorDeleteQueue() {
        return new Queue(DOCUMENT_VECTOR_DELETE_QUEUE, true);
    }

    @Bean
    public Binding documentVectorDeleteBinding(
            @Qualifier("documentVectorDeleteQueue") Queue queue,
            DirectExchange documentExchange
    ) {
        return BindingBuilder.bind(queue).to(documentExchange).with(DOCUMENT_VECTOR_DELETE_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
