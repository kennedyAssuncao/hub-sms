package com.hub.sms_gateway.service;

import com.hub.sms_gateway.dto.SmsRequest;
import com.hub.sms_gateway.dto.SmsResponse;
import com.hub.sms_gateway.entity.SmsMessage;
import com.hub.sms_gateway.gateway.SmsGateway;
import com.hub.sms_gateway.repository.SmsMessageRepository;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private final SmsGateway smsGateway;
    private final SmsMessageRepository repository;

    public SmsService(SmsGateway smsGateway, SmsMessageRepository repository) {
        this.smsGateway = smsGateway;
        this.repository = repository;
    }

    public SmsResponse send(SmsRequest request) {

        SmsMessage sms = new SmsMessage(request.phone(), request.message());

        repository.save(sms);

        try {
            String modemResponse = smsGateway.send(request.phone(),request.message());

            sms.markAsSent(modemResponse);

            repository.save(sms);

            return new SmsResponse(sms.getId(), sms.getStatus().name(), modemResponse);

        } catch (RuntimeException exception) {

            sms.markAsFailed(exception.getMessage());

            repository.save(sms);

            throw exception;
        }
    }
}