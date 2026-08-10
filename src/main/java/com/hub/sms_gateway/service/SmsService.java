package com.hub.sms_gateway.service;

import com.hub.sms_gateway.dto.SmsRequest;
import com.hub.sms_gateway.dto.SmsResponse;
import com.hub.sms_gateway.gateway.SmsGateway;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private final SmsGateway smsGateway;

    public SmsService(SmsGateway smsGateway) {
        this.smsGateway = smsGateway;
    }

    public SmsResponse send(SmsRequest request) {

        String modemResponse =
                smsGateway.send(
                        request.phone(),
                        request.message()
                );

        return new SmsResponse(
                "SENT",
                modemResponse
        );
    }
}
