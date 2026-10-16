# AI Product Recommendation Agent Specification

## Purpose

This document specifies the Product Recommendation AI agent to be implemented in the `product-service` module of this repository. The agent accepts a user's natural-language product request, consults repository-backed query tools (product repository and internal read-only service clients), and returns a validated recommendation response. The agent is strictly read-only and returns a well-structured JSON recommendation object.

When the request contains an explicit name, email address, or phone number the system MAY resolve identity to derive a minimal, non-identifying preference profile (product IDs and category counts) used only as an internal ranking signal. Identity or raw order history must never appear in the agent's returned JSON `message` or in the HTTP response.

The agent must not create, update, delete, cancel, or otherwise mutate customers, products, baskets, orders, notifications, events, or projections.

## Repository Context

- Services present: `product-service`, `customer-service`, `order-service`, `notification-service`.
- See `product-service/AI_MODEL_CONFIG.md` and `product-service/OLLAMA_SETUP.md` for existing AI provider notes and local model setup.
- Build system: Maven; service modules have their own `pom.xml` files (add dependencies only to `product-service/pom.xml`).

## Technology and Configuration

- Add LangChain4j and integration dependencies only to `product-service/pom.xml`, with versions pinned via a Maven property at the top of that file.
- Provide a single configurable `ChatLanguageModel` provider implementation (local Ollama or a cloud provider). Use the existing `product-service/AI_MODEL_CONFIG.md` to document environment variables and example configuration.
- Provider credentials and endpoints MUST be supplied via environment variables (e.g. `OLLAMA_URL`, `OLLAMA_MODEL`, `OPENAI_API_KEY`); do not commit secrets or log them.
- `product-service` must start and operate normally when AI is disabled or misconfigured; in those cases the recommendation endpoint MUST return `503 Service Unavailable` with a non-sensitive message.
- Configure internal service URLs via properties such as `order.service.url` and `customer.service.url` in `application.yml` / `application.properties` for `product-service`.
- Conversation memory is bounded and in-memory only, keyed by `sessionId`. Retain at most 20 messages per session and redact PII before storing. Memory must be ephemeral and cleared on restart.

## Implementation notes

- Implement the agent in `product-service` under a clear package such as `com.retail.product.ai`.
- Use a LangChain4j `@AiService` interface (e.g. `ProductRecommendationAgent`) with `@MemoryId String sessionId` and `@UserMessage String message` parameters.
- Expose a small set of Spring-managed LangChain4j tools in a `ProductRecommendationTools` component. Tools are thin wrappers over the product repository and the internal read-only clients described below.

## HTTP Contract

Expose the agent through `product-service` using an HTTP endpoint. Use the existing microservice REST style; the path should be:

- Method: `GET` or `POST` (recommend `POST` for longer messages)
- Path: `/product/recommendation`
- Request: `RecommendationRequest`
- Response: `RecommendationResponse`

`RecommendationRequest` example:

{
	"sessionId": "optional-client-session-id",
	"message": "I am Alex Smith and I need a gift for a keen home cook"
}

Validation rules:

- `sessionId` is optional; generate a UUID server-side when omitted.
- `message` is required and must contain 1–1000 non-whitespace characters.

Runtime contract with the LLM:

- The LLM-facing `@AiService` method must return a single JSON object string with the exact shape:
	{ "message": "...", "productIds": [1,2,3] }
- The `message` text is free-form but MUST only reference products, categories, prices or descriptions that appear in canonical product records returned by the product tools.
- The `productIds` array MUST contain numeric IDs that exist in the product repository. The service MUST validate and translate these IDs into canonical `ProductView` objects before returning the HTTP response. Unknown IDs are ignored.

`RecommendationResponse` example (HTTP response body):

{
	"sessionId": "uuid",
	"message": "A concise recommendation mentioning only catalogue items",
	"products": [
		{ "productId": 10, "name": "Chef Knife", "category": "Kitchen", "price": 49.99 }
	]
}

The HTTP response must NOT include any PII, order identifiers, internal prompts, model config, or model reasoning.

## Data Sources and Integration

- The agent must use `product-service`'s repository/query layer directly (not HTTP to itself) to fetch products by ID, category, partial name, or the complete catalogue as required.
- Add a read-only `OrderServiceClient` that calls Order Service query endpoints to obtain minimal preference signals (product IDs and category counts). Suggested endpoints:
	- `GET /order/customer/{customerId}`
	- `GET /order/customer/email/{email}`
	- `GET /order/customer/phone/{phone}`
- Add a read-only `CustomerServiceClient` for identity resolution. For name-based resolution only, add a private internal endpoint such as `GET /customer/name?firstName={firstName}&lastName={lastName}` on `customer-service`; do not expose this in public API docs and do not use it except for non-public personalization logic.
- Clients and tool methods MUST return only minimal ranking data (product IDs, category counts). Do not return raw order objects or customer records to the LLM or persist them in memory.

Name resolution rules:

- If a supplied name matches zero or multiple customers, do NOT use order history. Prompt the user for email or phone.
- If identity resolution fails or order history is unavailable, fall back to non-personalised recommendations and do not reveal the reason to the user.

## Recommendation Behaviour and System Prompt

The system prompt and tool contracts must enforce the following:

1. Use friendly, helpful, concise language.
2. Recommend only products returned by Product Service tools.
3. Include only product fields in results: `productId`, `name`, `category`, `price`.
4. Never invent products, prices, categories, availability, or customer/order details.
5. Treat supplied identifiers as internal signals only; never echo them back in responses.
6. Use session messages to interpret follow-ups and retain preferences within the session.
7. Ask a focused clarification when the request is ambiguous or when a name does not uniquely identify a customer.
8. Detect whether the request is personal or a gift recommendation and adapt ranking accordingly.
9. Treat PII and internal tool outputs as confidential; never send them to the model.
10. Decline non-product or data-disclosure requests politely and refuse to reveal PII.

## Ranking Rules

- Filter candidate products by stated needs: category, name fragments, price range, and explicit exclusions.
- Determine intent: personal vs gift. For personal recommendations, incorporate ordering-history signals with higher weight; for gifts, prioritise recipient constraints and stated preferences.
- Do not recommend a previously purchased product unless it independently matches the current request.
- Product relevance and explicit user constraints take precedence over ordering history.
- Return at most five products, ordered most-to-least relevant.
- When no suitable products are found, clearly say so and ask one focused clarification question.

## Privacy, Security and Logging

- PII may be used only transiently for the current request and must be redacted before storing in memory. Do not persist PII across restarts.
- Never send PII or raw order data to the LLM. Convert identity and order history into a minimal preference profile in application code before supplying signals to the model.
- Tools that access customer or order services MUST return only minimal aggregation (product IDs, category counts).
- Logs must contain only a correlation ID (generated session ID), outcome, and non-sensitive error category. Do NOT log prompts, PII, customer IDs, order data, or model prompts/responses.

## Error Handling

- `400 Bad Request` for invalid input (missing/blank `message` or invalid field values).
- `503 Service Unavailable` when AI is disabled, misconfigured, or no model provider is reachable; the message must be non-sensitive and not require API keys when AI is disabled.
- If product validation fails or internal services are unavailable, return a helpful clarification fallback; do not leak internal endpoints or stack traces.
- Identity-resolution failures must not block non-personalised recommendations.

## Tests and Acceptance Criteria

Add focused unit and integration tests in `product-service` covering:

- Missing or blank `message` returns `400`.
- Product responses include only `productId`, `name`, `category`, and `price` from canonical product data.
- Vague or ambiguous requests produce a friendly clarification question and do not invent products.
- Follow-up requests reuse session context when `sessionId` is unchanged; different `sessionId` receives no prior context.
- Supplying email or phone triggers `OrderServiceClient` to return a minimal preference profile that influences ranking.
- A uniquely resolved name may influence ranking; ambiguous/unknown names prompt for email/phone without revealing matches.
- Personal recommendations weight order-history preferences higher than gift recommendations for the same request and history.
- Responses never contain PII, customer IDs, order IDs, or raw ordering history.
- Auditor remediation path: when a model response is rejected, the server may requery the agent with `AUDIT_FEEDBACK:` and tests must cover both successful remediation and exhausted-retry fallback.
- Product-only recommendations still succeed when customer lookup fails.
- Unsafe or non-product requests do not invoke tools that can disclose internal data.
- AI-disabled configuration returns `503` and tests must not require an API key (use mocked `ChatLanguageModel`).

Tests must use mocked `ChatLanguageModel` and mocked internal clients; they must not call external LLMs or real customer/order services.

## Operational Notes

- Document AI configuration in `product-service/AI_MODEL_CONFIG.md` and local Ollama setup in `product-service/OLLAMA_SETUP.md`.
- Keep dependency and configuration changes isolated to `product-service`.

## Out of Scope

- Authentication and authorisation.
- Product purchasing, basket management, or command actions.
- Persistent conversation logs or cross-restart analytics.
- Stock, delivery, or order-tracking advice.
- Disclosure of personalised rationale or raw ordering history in responses.
