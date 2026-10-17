package com.retail.common;

import java.time.Instant;

public record ProjectionFailure(
        DomainEventMessage event,
        String errorType,
        String errorMessage,
        Instant failedAt) {
}
