package com.hub.sms_gateway.dto;

import com.hub.sms_gateway.entity.SmsMessage;
import com.hub.sms_gateway.entity.SmsStatus;

import java.time.LocalDateTime;

public record SmsHistoryItemResponse(
        Long id,
        String phone,
        String message,
        SmsStatus status,
        LocalDateTime createdAt,
        LocalDateTime sentAt,
        String errorMessage,
        String modemResponse
) {
    public static SmsHistoryItemResponse from(SmsMessage sms) {
        return new SmsHistoryItemResponse(sms.getId(), sms.getPhone(), sms.getMessage(),
                sms.getStatus(), sms.getCreatedAt(), sms.getSentAt(),
                sms.getErrorMessage(), sms.getModemResponse());
    }
}
