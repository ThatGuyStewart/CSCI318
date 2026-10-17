package com.retail.common;

import java.time.Instant;

public record DeserializationFailure(
        String sourceTopic,
        int sourcePartition,
        long sourceOffset,
        String keyBase64,
        String valueBase64,
        String errorType,
        String errorMessage,
        Instant failedAt) {
}
