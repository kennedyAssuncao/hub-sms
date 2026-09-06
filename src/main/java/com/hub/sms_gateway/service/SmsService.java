package com.hub.sms_gateway.service;

import com.hub.sms_gateway.dto.SmsRequest;
import com.hub.sms_gateway.dto.SmsHistoryItemResponse;
import com.hub.sms_gateway.dto.SmsHistoryResponse;
import com.hub.sms_gateway.entity.SmsStatus;
import com.hub.sms_gateway.exception.SmsNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional(readOnly = true)
    public SmsHistoryItemResponse findById(Long id) {
        return repository.findById(id)
                .map(SmsHistoryItemResponse::from)
                .orElseThrow(() -> new SmsNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public SmsHistoryResponse findAll(int page, int size, SmsStatus status) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var result = status == null
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        return new SmsHistoryResponse(result.getContent().stream().map(SmsHistoryItemResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
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
