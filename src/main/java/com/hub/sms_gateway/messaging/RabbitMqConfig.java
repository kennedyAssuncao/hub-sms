package com.hub.sms_gateway.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String SMS_EXCHANGE =
            "sms.send.exchange";

    public static final String SMS_QUEUE =
            "sms.send.queue";

    public static final String SMS_ROUTING_KEY =
            "sms.send";

    public static final String SMS_DLQ =
            "sms.send.dlq";

    public static final String SMS_DLX =
            "sms.send.dlx";

    public static final String SMS_RETRY_EXCHANGE =
            "sms.retry.exchange";

    public static final String SMS_RETRY_QUEUE =
            "sms.retry.queue";

    public static final String SMS_RETRY_ROUTING_KEY =
            "sms.retry";

    public static final int RETRY_DELAY_MS = 30_000;

    @Bean
    DirectExchange smsExchange() {
        return new DirectExchange(
                SMS_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    DirectExchange smsDeadLetterExchange() {
        return new DirectExchange(
                SMS_DLX,
                true,
                false
        );
    }

    @Bean
    Queue smsQueue() {

        return QueueBuilder
                .durable(SMS_QUEUE)

                .deadLetterExchange(SMS_DLX)

                .deadLetterRoutingKey(
                        SMS_DLQ
                )

                .build();
    }

    @Bean
    Queue smsDeadLetterQueue() {

        return QueueBuilder
                .durable(SMS_DLQ)
                .build();
    }

    @Bean
    Binding smsBinding(
            Queue smsQueue,
            DirectExchange smsExchange
    ) {

        return BindingBuilder
                .bind(smsQueue)
                .to(smsExchange)
                .with(SMS_ROUTING_KEY);
    }

    @Bean
    Binding smsDlqBinding(
            Queue smsDeadLetterQueue,
            DirectExchange smsDeadLetterExchange
    ) {

        return BindingBuilder
                .bind(smsDeadLetterQueue)
                .to(smsDeadLetterExchange)
                .with(SMS_DLQ);
    }

    @Bean
    JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    DirectExchange smsRetryExchange() {
        return new DirectExchange(
                SMS_RETRY_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    Queue smsRetryQueue() {

        return QueueBuilder.durable(SMS_RETRY_QUEUE)
                .ttl(RETRY_DELAY_MS)
                .deadLetterExchange(SMS_EXCHANGE)
                .deadLetterRoutingKey(SMS_ROUTING_KEY)
                .build();
    }

    @Bean
    Binding smsRetryBinding(Queue smsRetryQueue, DirectExchange smsRetryExchange ) {

        return BindingBuilder
                .bind(smsRetryQueue)
                .to(smsRetryExchange)
                .with(SMS_RETRY_ROUTING_KEY);
    }
}