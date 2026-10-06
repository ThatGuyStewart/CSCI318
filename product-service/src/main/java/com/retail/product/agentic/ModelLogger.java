package com.retail.product.agentic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;

public class ModelLogger implements ChatModelListener {

    private static final Logger log = LoggerFactory.getLogger(ModelLogger.class);
    // Default-off flag to opt-in to logging model request/response content.
    // Enable with system property `product.recommendation.logModelContent=true`.
    private static final boolean LOG_MODEL_CONTENT = Boolean.parseBoolean(System.getProperty("product.recommendation.logModelContent", "false"));

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        if (LOG_MODEL_CONTENT) {
            // Avoid truncating redacted content when developer explicitly enables content logging
            log.debug("onRequest(): {}", redactSensitive(requestContext.chatRequest(), false));
        } else {
            log.info("onRequest(): model request received");
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // Log non-sensitive metadata at INFO and content only when explicitly enabled.
        try {
            var resp = responseContext.chatResponse();
            String finishReason = null;
            Integer tokensOut = null;
            try {
                var fr = resp.metadata().finishReason();
                finishReason = fr == null ? null : fr.toString();
            } catch (Throwable ignore) {
            }
            try {
                tokensOut = resp.metadata().tokenUsage() == null ? null : resp.metadata().tokenUsage().outputTokenCount();
            } catch (Throwable ignore) {
            }
            log.info("Model finishReason={} tokensOut={}", finishReason, tokensOut);
        } catch (Throwable t) {
            log.info("onResponse(): model response received");
        }

        if (LOG_MODEL_CONTENT) {
            try {
                log.debug("onResponse(): {}", redactSensitive(responseContext.chatResponse(), false));
            } catch (Throwable ignore) {
            }
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        if (LOG_MODEL_CONTENT) {
            log.debug("onError(): {}", redactSensitive(errorContext.error().getMessage(), false), errorContext.error());
        } else {
            log.info("onError(): model error occurred");
        }
    }

    private String redactSensitive(Object obj) {
        return redactSensitive(obj, true);
    }

    private String redactSensitive(Object obj, boolean truncate) {
        if (obj == null) {
            return null;
        }
        String s = String.valueOf(obj);
        // redact emails
        s = s.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[REDACTED_EMAIL]");
        // redact long digit sequences (>=9 digits)
        s = s.replaceAll("\\b\\d{9,}\\b", "[REDACTED_NUMBER]");
        // redact UUID-like tokens
        s = s.replaceAll("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b", "[REDACTED_UUID]");
        if (truncate) {
            int max = 1024;
            if (s.length() > max) {
                s = s.substring(0, max) + "...[TRUNCATED]";
            }
        }
        return s;
    }
}
