package com.hub.sms_gateway.controller;

import com.hub.sms_gateway.dto.SmsRequest;
import com.hub.sms_gateway.dto.SmsResponse;
import com.hub.sms_gateway.service.SmsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sms")
public class SmsController {

    private final SmsService smsService;

    public SmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    @PostMapping
    public ResponseEntity<SmsResponse> send(
            @Valid @RequestBody SmsRequest request
    ) {

        return ResponseEntity.ok(
                smsService.send(request)
        );
    }
}
