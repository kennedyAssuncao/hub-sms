package com.hub.sms_gateway.dto;

import jakarta.validation.constraints.NotBlank;

public record SmsRequest(@NotBlank
                         String phone,

                         @NotBlank
                         String message) {
}
