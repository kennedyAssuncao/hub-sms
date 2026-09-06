package com.hub.sms_gateway.controller;

import com.hub.sms_gateway.dto.SmsRequest;
import com.hub.sms_gateway.dto.SmsHistoryItemResponse;
import com.hub.sms_gateway.dto.SmsHistoryResponse;
import com.hub.sms_gateway.entity.SmsStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
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

    @GetMapping
    public SmsHistoryResponse findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) SmsStatus status
    ) {
        return smsService.findAll(page, size, status);
    }

    @GetMapping("/{id}")
    public SmsHistoryItemResponse findById(@PathVariable Long id) {
        return smsService.findById(id);
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
