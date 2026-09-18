package com.hub.sms_gateway.service;

import com.hub.sms_gateway.dto.SmsRequest;
import com.hub.sms_gateway.dto.SmsResponse;
import com.hub.sms_gateway.entity.SmsMessage;
import com.hub.sms_gateway.entity.SmsStatus;
import com.hub.sms_gateway.messaging.SmsQueuePublisher;
import com.hub.sms_gateway.repository.SmsMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private final SmsMessageRepository repository;
    private final SmsQueuePublisher publisher;

    public SmsService(
            SmsMessageRepository repository,
            SmsQueuePublisher publisher
    ) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public SmsResponse send(SmsRequest request) {

        SmsMessage sms = new SmsMessage(
                request.phone(),
                request.message()
        );

        sms = repository.save(sms);

        publisher.publish(sms.getId());

        return new SmsResponse(
                sms.getId(),
                sms.getStatus().name(),
                null
        );
    }

    public Page<SmsMessage> findAll(
            int page,
            int size,
            SmsStatus status
    ) {

        Pageable pageable = PageRequest.of(page, size);

        if (status != null) {
            return repository.findByStatus(status, pageable);
        }

        return repository.findAll(pageable);
    }

    public SmsMessage findById(Long id) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "SMS não encontrado: " + id
                        )
                );
    }
}