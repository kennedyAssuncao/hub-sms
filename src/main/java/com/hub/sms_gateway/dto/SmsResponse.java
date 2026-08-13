package com.hub.sms_gateway.dto;

public record SmsResponse(
        Long id,
        String status,
        String modemResponse
) {
}