package com.hub.sms_gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SmsRequest(

        @NotBlank(message = "O telefone é obrigatório")
        @Pattern(
                regexp = "^\\+[1-9]\\d{7,14}$",
                message = "O telefone deve estar no formato internacional E.164, por exemplo +5512999999999"
        )
        String phone,

        @NotBlank(message = "A mensagem é obrigatória")
        String message

) {
}