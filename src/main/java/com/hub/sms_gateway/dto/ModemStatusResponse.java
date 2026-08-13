package com.hub.sms_gateway.dto;

public record ModemStatusResponse(
        boolean connected,
        String port,
        String manufacturer,
        String model,
        String operator,
        Integer signal,
        boolean simReady,
        boolean networkRegistered) {
}