package com.hub.sms_gateway.messaging;

import com.hub.sms_gateway.messaging.dto.SmsSendMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class SmsQueuePublisher {

    private final RabbitTemplate rabbitTemplate;

    public SmsQueuePublisher(
            RabbitTemplate rabbitTemplate
    ) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(Long smsId) {

        SmsSendMessage message =
                new SmsSendMessage(smsId);

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.SMS_EXCHANGE,
                RabbitMqConfig.SMS_ROUTING_KEY,
                message
        );
    }

    public void publishRetry(Long smsId) {

        SmsSendMessage message =
                new SmsSendMessage(smsId);

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.SMS_RETRY_EXCHANGE,
                RabbitMqConfig.SMS_RETRY_ROUTING_KEY,
                message
        );
    }
}