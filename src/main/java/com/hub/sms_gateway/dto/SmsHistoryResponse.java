package com.hub.sms_gateway.dto;

import java.util.List;

public record SmsHistoryResponse(
        List<SmsHistoryItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
