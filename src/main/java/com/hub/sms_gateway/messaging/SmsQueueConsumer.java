package com.hub.sms_gateway.messaging;

import com.hub.sms_gateway.entity.SmsMessage;
import com.hub.sms_gateway.entity.SmsStatus;
import com.hub.sms_gateway.gateway.SmsGateway;
import com.hub.sms_gateway.messaging.dto.SmsSendMessage;
import com.hub.sms_gateway.repository.SmsMessageRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SmsQueueConsumer {

    private final SmsMessageRepository repository;
    private final SmsGateway smsGateway;
    private final SmsQueuePublisher publisher;

    private static final int MAX_RETRIES = 3;

    public SmsQueueConsumer(
            SmsMessageRepository repository,
            SmsGateway smsGateway,
            SmsQueuePublisher publisher
    ) {
        this.repository = repository;
        this.smsGateway = smsGateway;
        this.publisher = publisher;
    }

    @RabbitListener( queues = RabbitMqConfig.SMS_QUEUE, concurrency = "1" )
    public void consume(SmsSendMessage message) {

        SmsMessage sms = repository.findById(message.smsId())
                .orElseThrow(() -> new IllegalStateException( "SMS não encontrado: " + message.smsId() ));

        if (sms.getStatus() != SmsStatus.PENDING
                && sms.getStatus() != SmsStatus.RETRYING) {
            return;
        }

        sms.markAsProcessing();
        repository.save(sms);

        try {

            String modemResponse = smsGateway.send(sms.getPhone(), sms.getMessage());
            sms.markAsSent(modemResponse);
            repository.save(sms);

        } catch (RuntimeException exception) {
            sms.incrementRetryCount();
            if (sms.getRetryCount() <= MAX_RETRIES) {
                sms.markAsRetrying( exception.getMessage());
                repository.save(sms);
                publisher.publishRetry(message.smsId());

                return;
            }

            sms.markAsFailed(exception.getMessage());

            repository.save(sms);

            throw exception;
        }
    }
}
