package com.researchflow.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitMqConfig {

    public static final String DOCUMENT_EXCHANGE = "researchflow.document";
    public static final String DOCUMENT_RETRY_EXCHANGE = "researchflow.document.retry";
    public static final String DOCUMENT_DEAD_LETTER_EXCHANGE = "researchflow.document.dlx";
    public static final String DOCUMENT_PARSE_QUEUE = "document.parse";
    public static final String DOCUMENT_PARSE_ROUTING_KEY = "document.parse";
    public static final String DOCUMENT_VECTORIZE_QUEUE = "document.vectorize";
    public static final String DOCUMENT_VECTORIZE_ROUTING_KEY = "document.vectorize";
    public static final String DOCUMENT_VECTOR_DELETE_QUEUE = "document.vector.delete";
    public static final String DOCUMENT_VECTOR_DELETE_ROUTING_KEY = "document.vector.delete";
    public static final int[] RETRY_DELAYS_MILLIS = {2_000, 10_000, 30_000};

    @Bean
    public DirectExchange documentExchange() {
        return new DirectExchange(DOCUMENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange documentRetryExchange() {
        return new DirectExchange(DOCUMENT_RETRY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange documentDeadLetterExchange() {
        return new DirectExchange(DOCUMENT_DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue documentParseQueue() {
        return new Queue(DOCUMENT_PARSE_QUEUE, true);
    }

    @Bean
    public Binding documentParseBinding(
            @Qualifier("documentParseQueue") Queue documentParseQueue,
            @Qualifier("documentExchange") DirectExchange documentExchange
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
            @Qualifier("documentExchange") DirectExchange documentExchange
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
            @Qualifier("documentExchange") DirectExchange documentExchange
    ) {
        return BindingBuilder.bind(queue).to(documentExchange).with(DOCUMENT_VECTOR_DELETE_ROUTING_KEY);
    }

    /**
     * Retry queues use fixed TTL tiers and route expired messages back to the main exchange.
     * Main queue definitions deliberately remain unchanged so existing installations can upgrade
     * without deleting durable queues first.
     */
    @Bean
    public Declarables documentRetryAndDeadLetterTopology(
            @Qualifier("documentRetryExchange") DirectExchange retryExchange,
            @Qualifier("documentDeadLetterExchange") DirectExchange deadLetterExchange
    ) {
        List<Declarable> declarables = new ArrayList<>();
        addRecoveryTopology(declarables, retryExchange, deadLetterExchange,
                DOCUMENT_PARSE_QUEUE, DOCUMENT_PARSE_ROUTING_KEY);
        addRecoveryTopology(declarables, retryExchange, deadLetterExchange,
                DOCUMENT_VECTORIZE_QUEUE, DOCUMENT_VECTORIZE_ROUTING_KEY);
        addRecoveryTopology(declarables, retryExchange, deadLetterExchange,
                DOCUMENT_VECTOR_DELETE_QUEUE, DOCUMENT_VECTOR_DELETE_ROUTING_KEY);
        return new Declarables(declarables);
    }

    private void addRecoveryTopology(
            List<Declarable> declarables,
            DirectExchange retryExchange,
            DirectExchange deadLetterExchange,
            String queueName,
            String mainRoutingKey
    ) {
        String deadRoutingKey = queueName + ".dead";
        Queue deadLetterQueue = QueueBuilder.durable(queueName + ".dlq").build();
        declarables.add(deadLetterQueue);
        declarables.add(BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(deadRoutingKey));

        for (int index = 0; index < RETRY_DELAYS_MILLIS.length; index++) {
            int attempt = index + 1;
            String retryRoutingKey = queueName + ".retry." + attempt;
            Queue retryQueue = QueueBuilder.durable(retryRoutingKey)
                    .ttl(RETRY_DELAYS_MILLIS[index])
                    .deadLetterExchange(DOCUMENT_EXCHANGE)
                    .deadLetterRoutingKey(mainRoutingKey)
                    .build();
            declarables.add(retryQueue);
            declarables.add(BindingBuilder.bind(retryQueue).to(retryExchange).with(retryRoutingKey));
        }
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public ApplicationRunner initializeRabbitTopology(RabbitAdmin rabbitAdmin) {
        return args -> rabbitAdmin.initialize();
    }
}
