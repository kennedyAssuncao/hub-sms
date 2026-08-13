package com.hub.sms_gateway.controller;

import com.hub.sms_gateway.dto.ModemStatusResponse;
import com.hub.sms_gateway.service.ModemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/modem")
public class ModemController {

    private final ModemService modemService;

    public ModemController(ModemService modemService) {
        this.modemService = modemService;
    }

    @GetMapping("/status")
    public ResponseEntity<ModemStatusResponse> getStatus() {

        return ResponseEntity.ok(
                modemService.getStatus()
        );
    }
}