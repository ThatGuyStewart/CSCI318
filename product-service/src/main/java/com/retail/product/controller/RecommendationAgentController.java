package com.retail.product.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.product.domain.ProductView;
import com.retail.product.dto.ErrorResponse;
import com.retail.product.dto.ProductResponse;
import com.retail.product.dto.RecommendationResponse;
import com.retail.product.exception.BadRequestException;
import com.retail.product.repository.ProductViewRepository;
import com.retail.product.service.ProductService;
import com.retail.product.service.RecommendationAgent;

@RestController
public class RecommendationAgentController {

    private static final Logger log = LoggerFactory.getLogger(RecommendationAgentController.class);
    private static final int MAX_PRODUCTS = 5;
    // Default-off flag to opt-in to logging raw model replies. Set system property
    // `product.recommendation.logRawReplies=true` to enable. Logging remains at
    // DEBUG level and replies are redacted before being emitted to logs.
    private static final boolean LOG_RAW_REPLIES = Boolean.parseBoolean(System.getProperty("product.recommendation.logRawReplies", "false"));

    private final ObjectProvider<RecommendationAgent> recommendationAgentProvider;
    private final ProductViewRepository productViewRepository;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public RecommendationAgentController(ObjectProvider<RecommendationAgent> recommendationAgentProvider,
            ProductViewRepository productViewRepository, ProductService productService, ObjectMapper objectMapper) {
        this.recommendationAgentProvider = recommendationAgentProvider;
        this.productViewRepository = productViewRepository;
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/product/recommendation")
    public ResponseEntity<?> getProductRecommendations(
            @RequestParam(required = false) String sessionId,
            @RequestParam String message) {
        if (message == null || message.isBlank() || message.length() > 1000) {
            throw new BadRequestException("message is required and must contain 1-1000 non-whitespace characters");
        }

        RecommendationAgent recommendationAgent;
        try {
            recommendationAgent = recommendationAgentProvider.getIfAvailable();
        } catch (RuntimeException ex) {
            log.info("Recommendation request rejected: AI recommendation service misconfigured");
            recommendationAgent = null;
        }
        if (recommendationAgent == null) {
            log.info("Recommendation request rejected: AI recommendation service unavailable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("SERVICE_UNAVAILABLE", "Product recommendations are currently unavailable"));
        }

        String resolvedSessionId = (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId;

        String rawReply;
        try {
            rawReply = recommendationAgent.recommend(resolvedSessionId, message);
        } catch (RuntimeException ex) {
            log.info("Recommendation request failed: model invocation error");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("SERVICE_UNAVAILABLE", "Product recommendations are currently unavailable"));
        }

        // Extract the first JSON object from the model output to tolerate appended prose or tool metadata.
        String firstJson = extractFirstJsonObject(rawReply);
        // Do not log raw model output at INFO to avoid leaking sensitive data.
        // When explicitly enabled, redact before logging and use DEBUG level.
        if (LOG_RAW_REPLIES) {
            // When explicitly enabled for debugging, avoid truncating the
            // redacted model reply so developers can inspect full context.
            log.debug("RAW REPLY FROM MODEL: {}", redactSensitive(rawReply, false));
        }

        // Validate model output is valid JSON matching expected schema; if not, retry once with a strict instruction.
        if (!isValidAgentJson(firstJson)) {
            log.info("Model returned invalid JSON; retrying once with strict instruction");
            try {
                String retryPrompt = message + "\n\nYour previous response was not a valid response object. Please reply with a single JSON object containing exactly two fields: 'message' and 'productIds'. Do not output raw tool calls or use nested JSON inside the message field.";
                rawReply = recommendationAgent.recommend(resolvedSessionId, retryPrompt);
                firstJson = extractFirstJsonObject(rawReply);
            } catch (RuntimeException ex) {
                log.info("Recommendation retry failed: model invocation error");
            }
        }

        return ResponseEntity.ok(toRecommendationResponse(resolvedSessionId, firstJson, message, recommendationAgent));
    }

    private RecommendationResponse toRecommendationResponse(String sessionId, String rawReply, String userMessage, RecommendationAgent recommendationAgent) {
        String replyMessage = "";
        List<Long> productIds = new ArrayList<>();

        try {
            String firstJson = extractFirstJsonObject(rawReply);
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(firstJson);

            // message extraction (tolerate different field types)
            if (root.has("message")) {
                replyMessage = root.get("message").isTextual() ? root.get("message").asText() : objectMapper.writeValueAsString(root.get("message"));
            } else if (root.has("reply")) {
                replyMessage = root.get("reply").asText("");
            } else if (root.has("response")) {
                replyMessage = root.get("response").asText("");
            } else if (root.has("text")) {
                replyMessage = root.get("text").asText("");
            } else if (root.has("answer")) {
                replyMessage = root.get("answer").asText("");
            } else if (root.has("explanation")) {
                replyMessage = root.get("explanation").asText("");
            }
            replyMessage = sanitizeMessage(replyMessage);

            // productIds extraction: handle array, numeric, stringified array, or comma-separated string
            com.fasterxml.jackson.databind.JsonNode idsNode = root.get("productIds");
            if (idsNode != null) {
                if (idsNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode n : idsNode) {
                        if (n.isNumber()) {
                            productIds.add(n.longValue());
                        } else if (n.isTextual()) {
                            String txt = n.asText();
                            // tolerate textual tokens like "ID=1" or "product: 2"
                            List<Long> nums = extractLongsFromText(txt);
                            if (!nums.isEmpty()) {
                                productIds.addAll(nums);
                            } else {
                                try {
                                    productIds.add(Long.parseLong(txt));
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                    }
                } else if (idsNode.isNumber()) {
                    productIds.add(idsNode.longValue());
                } else if (idsNode.isTextual()) {
                    String txt = idsNode.asText().trim();
                    // try parse as JSON array string like "[42]"
                    if (txt.startsWith("[") && txt.endsWith("]")) {
                        try {
                            com.fasterxml.jackson.databind.JsonNode arr = objectMapper.readTree(txt);
                            if (arr.isArray()) {
                                for (com.fasterxml.jackson.databind.JsonNode n : arr) {
                                    if (n.isNumber()) {
                                        productIds.add(n.longValue());
                                    } else if (n.isTextual()) {
                                        try {
                                            productIds.add(Long.parseLong(n.asText()));
                                        } catch (NumberFormatException ignored) {
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    } else {
                        // try comma/space separated numbers
                        String[] parts = txt.split("[^0-9]+");
                        for (String p : parts) {
                            if (p == null || p.isBlank()) {
                                continue;
                            }
                            try {
                                productIds.add(Long.parseLong(p));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }

            // fallback: single productId field
            if (productIds.isEmpty() && root.has("productId")) {
                com.fasterxml.jackson.databind.JsonNode p = root.get("productId");
                if (p.isNumber()) {
                    productIds.add(p.longValue());
                } else if (p.isTextual()) {
                    List<Long> nums = extractLongsFromText(p.asText());
                    if (!nums.isEmpty()) {
                        productIds.addAll(nums);
                    } else {
                        try {
                            productIds.add(Long.parseLong(p.asText()));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            // Additional fallback: if no productIds were parsed, attempt to find
            // a numeric JSON array anywhere in the raw reply (e.g. "[7]" or "[1,2]")
            // Some model outputs embed the array separately or the streaming
            // extraction may have missed it; this is a conservative best-effort.
            if (productIds.isEmpty() && rawReply != null) {
                try {
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\[\\s*\\d+(?:\\s*,\\s*\\d+)*\\s*\\]");
                    java.util.regex.Matcher m = p.matcher(rawReply);
                    if (m.find()) {
                        String arrText = m.group();
                        com.fasterxml.jackson.databind.JsonNode arrNode = objectMapper.readTree(arrText);
                        if (arrNode.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode n : arrNode) {
                                if (n.isNumber()) {
                                    productIds.add(n.longValue());
                                } else if (n.isTextual()) {
                                    try {
                                        productIds.add(Long.parseLong(n.asText()));
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            // If the agent's message appears truncated, attempt to finish it (same logic as before)
            if (isLikelyTruncated(replyMessage)) {
                log.info("Agent message appears truncated; requesting completion");
                boolean completed = false;
                try {
                    String finishPrompt = "The previous JSON response was truncated. Reply with a single JSON object only, identical to the previous but with the 'message' field completed. Do not include any other text.";
                    String finishRaw = recommendationAgent.recommend(sessionId, finishPrompt);
                    String finishJson = extractFirstJsonObject(finishRaw);
                    com.fasterxml.jackson.databind.JsonNode finished = objectMapper.readTree(finishJson);
                    if (finished.has("message")) {
                        replyMessage = finished.get("message").asText();
                    }
                    com.fasterxml.jackson.databind.JsonNode finishedIds = finished.get("productIds");
                    if (finishedIds != null && productIds.isEmpty()) {
                        if (finishedIds.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode n : finishedIds) {
                                if (n.isNumber()) {
                                    productIds.add(n.longValue());
                                }
                            }
                        } else if (finishedIds.isNumber()) {
                            productIds.add(finishedIds.longValue());
                        }
                    }
                    completed = true;
                } catch (Exception ex) {
                    log.info("Agent completion attempt failed, using original truncated message");
                }

                if (!completed && productIds.isEmpty()) {
                    replyMessage = "I couldn't construct a proper response. Could you reword your request?";
                }
            }

        } catch (Exception ex) {
            log.info("Recommendation request could not parse model output: {}", ex.getMessage());
            // Sanitize the raw prose fallback too — the model may have included quotes
            replyMessage = sanitizeMessage((rawReply == null) ? "" : rawReply);
            
            // Attempt to recover product IDs from the raw reply as a best-effort fallback.
            // First, try to find a numeric JSON array like "[7]" anywhere in the reply.
            List<Long> recovered = new ArrayList<>();
            try {
                if (rawReply != null) {
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\[\\s*\\d+(?:\\s*,\\s*\\d+)*\\s*\\]");
                    java.util.regex.Matcher m = p.matcher(rawReply);
                    if (m.find()) {
                        String arrText = m.group();
                        com.fasterxml.jackson.databind.JsonNode arrNode = objectMapper.readTree(arrText);
                        if (arrNode.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode n : arrNode) {
                                if (n.isNumber()) {
                                    recovered.add(n.longValue());
                                } else if (n.isTextual()) {
                                    try {
                                        recovered.add(Long.parseLong(n.asText()));
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            // If that failed, fall back to extracting any numeric tokens from the reply text.
            if (recovered.isEmpty()) {
                recovered.addAll(extractLongsFromText(rawReply == null ? "" : rawReply));
            }

            productIds = recovered;
        }

        List<ProductResponse> products = new ArrayList<>();
        for (Long productId : productIds) {
            if (products.size() >= MAX_PRODUCTS) {
                break;
            }
            ProductView productView = productViewRepository.findById(productId).orElse(null);
            if (productView != null) {
                products.add(productService.toProductResponse(productView));
            }
        }

        // Do not synthesize or hardcode controller replies. Return the agent's message (may be empty).
        return new RecommendationResponse(sessionId, replyMessage, products);
    }

    /**
     * Strips model-artifact backslash escapes from a plain-text message.
     * Jackson's asText() already handles legitimate JSON unescaping; any
     * remaining backslash sequences (e.g. \' or \") are produced by the LLM
     * incorrectly escaping characters inside a JSON string value and should be
     * removed.
     */
    private String sanitizeMessage(String message) {
        if (message == null) {
            return "";
        }
        // Remove backslash before apostrophes, quotes, and backticks (model escape artifacts).
        // Jackson handles re-escaping of any " characters when serializing the response.
        String cleaned = message.replaceAll("\\\\(['\"\\\\`])", "$1");
        // Strip backtick characters (model formatting artifacts)
        cleaned = cleaned.replace("`", "'").replace("\"", "'");
        return cleaned.trim();
    }

    /**
     * Redacts potentially sensitive pieces from model replies prior to logging.
     * This is a conservative, best-effort sanitizer: it removes long digit
     * sequences (possible account numbers), email-like tokens, and long
     * UUID-like tokens. Keep this logic intentionally simple to avoid false
     * negatives; callers must still avoid logging at INFO and only enable via
     * opt-in flag.
     */
    private String redactSensitive(String s) {
        return redactSensitive(s, true);
    }

    private String redactSensitive(String s, boolean truncate) {
        if (s == null) {
            return null;
        }
        String out = s;
        // redact emails
        out = out.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[REDACTED_EMAIL]");
        // redact long digit sequences (>=9 digits) which may be identifiers or CC-like
        out = out.replaceAll("\\b\\d{9,}\\b", "[REDACTED_NUMBER]");
        // redact UUID-like tokens
        out = out.replaceAll("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b", "[REDACTED_UUID]");
        if (truncate) {
            // truncate extremely long replies only when truncation is allowed
            int max = 1024;
            if (out.length() > max) {
                out = out.substring(0, max) + "...[TRUNCATED]";
            }
        }
        return out;
    }

    private record AgentReply(String message, List<Long> productIds) {

    }

    // Controller intentionally does not synthesise agent-style replies. The model's message is returned as-is.
    private boolean isValidAgentJson(String rawReply) {
        if (rawReply == null || rawReply.isBlank()) {
            return false;
        }
        try {
            // tolerate additional text by extracting first JSON object
            String firstJson = extractFirstJsonObject(rawReply);
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(firstJson);

            // Must be an object, and must contain at least message or productIds.
            // Reject hallucinated raw tool calls that lack these fields.
            return root != null && root.isObject()
                    && (root.has("message") || root.has("reply") || root.has("productIds") || root.has("productId"));
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isLikelyTruncated(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String m = message.trim();
        if (m.isEmpty()) {
            return false;
        }
        // Simple heuristic: ends with an unfinished word, an ellipsis (.. or ...), or very short token
        return m.matches(".*\\b(\\.\\.|\\.\\.\\.|[a-zA-Z]{1,3})$") || m.endsWith("\"") || m.endsWith("\\");
    }

    // Extracts the first JSON object or array from the given string. Uses a
    // streaming Jackson parser to avoid miscounting braces inside string values
    // and to tolerate surrounding prose. If streaming parse fails, falls back
    // to conservative heuristics for objects and arrays before returning the
    // original string.
    private String extractFirstJsonObject(String s) {
        if (s == null) {
            return null;
        }
        // First attempt: streaming parse to find either an object or an array
        try {
            com.fasterxml.jackson.core.JsonFactory factory = objectMapper.getFactory();
            try (com.fasterxml.jackson.core.JsonParser parser = factory.createParser(s)) {
                com.fasterxml.jackson.core.JsonToken token;
                while ((token = parser.nextToken()) != null) {
                    if (token == com.fasterxml.jackson.core.JsonToken.START_OBJECT || token == com.fasterxml.jackson.core.JsonToken.START_ARRAY) {
                        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(parser);
                        return objectMapper.writeValueAsString(node);
                    }
                }
            }
        } catch (Exception ex) {
            // fall through to heuristic fallback below
        }

        // Fallback A: try to find a balanced object using brace counting
        int objStart = s.indexOf('{');
        if (objStart >= 0) {
            int depth = 0;
            boolean inString = false;
            for (int i = objStart; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '"') {
                    // toggle inString unless escaped
                    boolean escaped = i > 0 && s.charAt(i - 1) == '\\' && !isEscapedBackslash(s, i - 1);
                    if (!escaped) {
                        inString = !inString;
                    }
                }
                if (!inString) {
                    if (c == '{') {
                        depth++;
                    } else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            return s.substring(objStart, i + 1);
                        }
                    }
                }
            }
        }

        // Fallback B: try to find a balanced array using simple bracket counting
        int arrStart = s.indexOf('[');
        if (arrStart >= 0) {
            int depth = 0;
            boolean inString = false;
            for (int i = arrStart; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '"') {
                    boolean escaped = i > 0 && s.charAt(i - 1) == '\\' && !isEscapedBackslash(s, i - 1);
                    if (!escaped) {
                        inString = !inString;
                    }
                }
                if (!inString) {
                    if (c == '[') {
                        depth++;
                    } else if (c == ']') {
                        depth--;
                        if (depth == 0) {
                            return s.substring(arrStart, i + 1);
                        }
                    }
                }
            }
        }

        // Nothing found; return original string as a last resort
        return s;
    }

    // Helper to detect whether the backslash at index pos is itself escaped
    private boolean isEscapedBackslash(String s, int pos) {
        int count = 0;
        for (int i = pos; i >= 0; i--) {
            if (s.charAt(i) == '\\') {
                count++;
            } else {
                break;
            }
        }
        return count % 2 == 1;
    }

    // Extract all long integers from a text token. Used to tolerate malformed
    // product id tokens like "ID=1" or "product: 42" inside model outputs.
    private List<Long> extractLongsFromText(String text) {
        List<Long> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\d+");
        java.util.regex.Matcher m = p.matcher(text);
        while (m.find()) {
            String g = m.group();
            try {
                out.add(Long.parseLong(g));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }
}
