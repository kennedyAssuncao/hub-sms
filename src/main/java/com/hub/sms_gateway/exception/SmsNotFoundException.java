package com.hub.sms_gateway.exception;

public class SmsNotFoundException extends RuntimeException {
    public SmsNotFoundException(Long id) {
        super("SMS com ID " + id + " não encontrado.");
    }
}
