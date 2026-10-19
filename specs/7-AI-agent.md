# AI Product Recommendation Agent Specification

## Purpose and Implemented Scope

Product Service implements a read-only, conversational product recommendation agent. It accepts natural-language requests, uses catalogue query tools and optional customer-preference summaries, and returns a message plus at most five products from the current `ProductView` repository.

The agent does not modify customers, products, baskets, orders, notifications, events or projections. It is not a natural-language command interface for purchasing or basket management.

## Implementation and Dependencies

- `com.retail.product.service.RecommendationAgent` is a LangChain4j AI Service interface using `@SystemMessage`, `@MemoryId` and `@UserMessage`. It is built programmatically with `AiServices`, not an `@AiService`-annotated component.
- `com.retail.product.agentic.AgentConfiguration` wires the model, tools, in-memory embedding store and conversation memory.
- `Tools` exposes read-only `@Tool` methods. It reads Product projections directly and uses `RestTemplate` for Customer and Order queries; dedicated `CustomerServiceClient` / `OrderServiceClient` abstractions are not implemented for this agent.
- `LangChain4jProductIngestor` populates the embedding store on startup.
- `RetryingRecommendationAgent` retries agent construction every five seconds when no `ChatModel` bean is available. This is not a guarantee of recovery from every provider startup or network failure.
- Dependencies are in `product-service/pom.xml`: LangChain4j core is `1.0.0`; provider starters and the AllMiniLmL6V2 embedding integration use `1.0.0-beta5`.

## Provider Configuration

The current model API is `ChatModel`. Spring profiles select the chat provider:

| Setting | Default / behaviour |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `ollama`; use `gemini` for Gemini |
| `AI_RECOMMENDATION_ENABLED` | `true`; maps to `retail.ai.recommendation.enabled` |
| `OLLAMA_BASE_URL` | `http://localhost:11434` |
| `OLLAMA_MODEL_NAME` | `llama3.2:3b` |
| `GEMINI_API_KEY` | Supplied through the environment for Gemini |
| `GEMINI_MODEL_NAME` | `gemini-3.5-flash-lite`; override with a tool-capable model supported by your key |

Both configured chat providers use temperature `0.0` and a 512-token output limit. Ollama's timeout is 600 seconds; Gemini's is 60 seconds. These are configuration defaults, not guarantees of deterministic replies or provider availability.

The `start-all-gemini.bat`, `.ps1` and `.sh` wrappers select the Gemini profile and delegate to the existing launcher. They do not set API keys. `AI_MODEL_PROVIDER`, `AI_MODEL_API_KEY`, `OLLAMA_URL`, `OLLAMA_MODEL` and OpenAI provider selection are not wired options in the current configuration.

The default embedding model is local ONNX AllMiniLmL6V2 when no other `EmbeddingModel` bean exists. Gemini chat selection does not change this embedding model. Provider credentials must not be committed to source control.

## HTTP Contract

- Method/path: `GET /product/recommendation`
- Required query parameter: `message`
- Optional query parameter: `sessionId`
- No JSON request body is consumed. `RecommendationRequest` exists as a DTO but is not bound by this endpoint.
- Missing, blank or longer-than-1000-character messages return `400`. The limit is the Java string length, not a count of non-whitespace characters.
- An omitted or blank `sessionId` generates a UUID. Any supplied nonblank session string is accepted; UUID syntax is not validated.

Example (Bash):

```bash
curl -G "http://localhost:8082/product/recommendation" --data-urlencode "message=I need a gift for a keen home cook"
```

Example response:

```json
{
  "sessionId": "generated-or-client-supplied-session-id",
  "message": "A concise recommendation",
  "products": [
    {
      "productId": 10,
      "name": "Chef Knife",
      "category": "Kitchen",
      "price": 49.99,
      "description": "A catalogue product description"
    }
  ]
}
```

Responses use `ProductResponse`, including `description`. Returned product IDs are looked up against the current projection; unknown IDs are omitted and at most five products are returned.

## Tools, Personalisation and Conversation

- Product tools provide embedding search, category listing, category availability and individual product lookup.
- Customer profile tools resolve supplied customer ID, email or phone using Customer Service, then query `GET /order/customer/{customerId}` in Order Service.
- Profile tools summarise category/product counts; an order-history-summary tool also returns order count and approximate spending. They do not intentionally return raw customer records or order lists to the model.
- Name-based customer lookup and a private customer-name endpoint are not implemented.
- The prompt prioritises the current product request and uses preferences only when requirements cannot be determined from the current message and identifying details are supplied.
- The model is instructed to search, verify products, rank relevant candidates and return only `{"message":"...","productIds":[...]}`.
- Conversation memory uses `MessageWindowChatMemory`, at most 20 messages per session, in process only. History is lost on restart. This is not a global bound on the number of sessions.

These ranking and privacy instructions are prompt-level behaviour, not a deterministic ranking engine or an authorisation boundary.

## Embedding Lifecycle

- Startup ingestion indexes existing `ProductView` records into an in-memory store, keyed by product ID.
- Product creation and updates attempt to replace the embedding immediately in the command path. Embedded text contains name, category and description, not price.
- Create/update embedding refresh is best-effort; failures do not fail the product operation and are logged at DEBUG. The embedding change is not deferred until database commit.
- Product deletion removes its embedding after database commit using the supported store API. A rolled-back deletion retains it. Removal failure is logged at WARN with product ID and exception type and does not undo the committed deletion.
- Kafka projection application and offline projection replay do not refresh this index themselves. The embedding store is not persisted across restarts.
- Embedding failures can fall back to deterministic placeholder vectors; this does not provide equivalent semantic quality to the real model.

## Output Processing and Errors

- The controller extracts the first balanced JSON object to tolerate appended text or metadata.
- Invalid output is retried once with a stricter instruction. Parsing also accepts several alternate fields/ID formats, and a separate completion attempt can occur for truncated replies.
- If the agent is disabled, bean construction fails, or the initial model invocation fails, the endpoint returns `503` with a non-sensitive unavailable message.
- Malformed output after retries can still produce a `200` fallback response; there is no implemented `AUDIT_FEEDBACK:` remediation protocol.
- Catalogue lookup validates the returned product list, not all claims in the generated explanation. It does not guarantee that prose is accurate, relevant or free of personal information.
- Customer/order lookup failures can be returned to the model as tool error strings; a uniform, independently verified clarification response is not guaranteed.

## Privacy and Logging Boundaries

- User messages are sent to the chat model and retained in ephemeral session memory. They may contain PII; there is no general pre-model or pre-memory redaction step.
- Tool summaries reduce data exposure but do not establish irreversible anonymisation. Prompts prohibit disclosure of customer/order details; these instructions are not a hard privacy guarantee.
- Authentication, customer-identity verification and session ownership checks are not implemented. Use synthetic data in demos, especially with a remote provider.
- Model-content logging is off by default. JVM system properties `product.recommendation.logRawReplies` and `product.recommendation.logModelContent` opt into DEBUG logging in the controller/listener.
- Existing logging redaction is best-effort (including email-like strings, long numbers and UUIDs); it does not cover all PII. Keep content logging disabled outside controlled development.
- LangChain4j logging defaults to INFO; Gemini request/response logging defaults to false.
- Some tool fallback paths log the search query and matched product names at INFO independently of the model-content switches. These switches therefore do not guarantee that user-derived text never reaches logs.

## Existing Tests and Remaining Work

`RecommendationAgentControllerTest` and `RecommendationAgentControllerIntegrationTest` cover request validation, agent availability and model-output handling using a mocked recommendation agent. Product integration tests cover embedding removal after deletion and retention on transaction rollback.

These tests are not live-provider, recommendation-quality, privacy-proof or multi-service end-to-end evidence. Pre-model PII redaction, verified identity/session access, stricter output auditing, transactional create/update indexing, replay-driven index refresh and live-provider evaluation remain separate enhancements.

## Out of Scope

- Purchasing, basket management or other write tools.
- External stock, delivery or order-tracking advice.
- Persistent conversation logs or cross-restart memory.
- Automatic implementation of the proposed trending-product and revenue analytics.
