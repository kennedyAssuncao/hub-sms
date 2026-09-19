package com.hub.sms_gateway.service;

import com.hub.sms_gateway.dto.SmsHistoryItemResponse;
import com.hub.sms_gateway.dto.SmsHistoryResponse;
import com.hub.sms_gateway.dto.SmsRequest;
import com.hub.sms_gateway.dto.SmsResponse;
import com.hub.sms_gateway.entity.SmsMessage;
import com.hub.sms_gateway.entity.SmsStatus;
import com.hub.sms_gateway.exception.SmsNotFoundException;
import com.hub.sms_gateway.messaging.SmsQueuePublisher;
import com.hub.sms_gateway.repository.SmsMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

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

        SmsMessage sms =
                new SmsMessage(
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

    public SmsHistoryResponse findAll(
            int page,
            int size,
            SmsStatus status
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<SmsMessage> result;

        if (status != null) {
            result = repository.findByStatus(status, pageable);
        } else {
            result = repository.findAll(pageable);
        }

        List<SmsHistoryItemResponse> content =
                result.getContent()
                        .stream()
                        .map(SmsHistoryItemResponse::from)
                        .toList();

        return new SmsHistoryResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public SmsHistoryItemResponse findById(Long id) {

        SmsMessage sms = repository.findById(id)
                .orElseThrow(() -> new SmsNotFoundException(id));

        return SmsHistoryItemResponse.from(sms);
    }
}
