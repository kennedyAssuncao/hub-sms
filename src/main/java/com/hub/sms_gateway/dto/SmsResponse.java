package com.hub.sms_gateway.dto;

public record SmsResponse(
        String status,
        String modemResponse
) {
}
