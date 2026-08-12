package com.hub.sms_gateway.util;

import java.text.Normalizer;

public final class SmsTextNormalizer {

    private SmsTextNormalizer() {
    }

    public static String normalize(String message) {

        if (message == null) {
            return null;
        }

        return Normalizer
                .normalize(message, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}