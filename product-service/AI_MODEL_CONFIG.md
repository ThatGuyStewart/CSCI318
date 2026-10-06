Goal
- Reduce hallucination, partial/truncated replies and make agent output deterministic.

Recommended model parameters (apply to your ChatModel provider / starter):
- temperature: 0.0 (fully deterministic)
- maxOutputTokens: 512 (allow enough space for full JSON reply)
- stop sequences: set provider-specific stop sequences to prevent appended text

Spring Boot property examples (provider-agnostic)
- Add these to `application.properties` or export as environment variables for your provider:

# Enable recommendation agent
retail.ai.recommendation.enabled=true

# Recommended model tuning (provider-specific; see below)
ai.model.temperature=0.0
ai.model.max-output-tokens=512
ai.model.stop-sequences=\n

Provider-specific notes
- Ollama (langchain4j-ollama-starter):
  - The ollama starter lets you configure model options in the ChatModel bean or when invoking the model. Consult the starter docs for how to pass `temperature` and `max_output_tokens` for your model. If a direct builder method is not available, set the options where the starter exposes them (environment or configuration map).
- Google Gemini (langchain4j-google-ai-gemini-starter):
  - Configure model parameters via the Gemini client options or the starter's configuration properties. Use a deterministic temperature and increased `maxOutputTokens`.

Implementation approaches
1. Preferred (recommended): configure the provider's ChatModel bean with `temperature=0.0` and `maxOutputTokens=512` so all agent calls inherit deterministic settings.
2. Fallback: if the starter does not expose builder options, configure the model via environment variables or provider-specific client config and restart the service.
3. Controller-side safety: we already added a one-shot retry when the model returns invalid JSON. Keep logging enabled (`ModelLogger`) so you can inspect raw model responses and tune settings.

Testing
1. Rebuild and start the service:

mvn -DskipTests package
java -jar product-service/target/product-service-1.0.0.jar.original

2. Reproduce the earlier curl test:

curl -X GET "http://localhost:8082/product/recommendation?message=I%20need%20a%20gift%20for%20a%20keen%20home%20cook"

3. If you still see truncated messages or frequent invalid JSON:
- Increase `maxOutputTokens` to 768–1024.
- Inspect raw responses in the logs (ModelLogger) to diagnose truncation or extra text.

References
- langchain4j starters documentation (ollama / google-ai-gemini): consult the starter README for provider-specific configuration keys and examples.

If you want, I can attempt to wire provider-specific options programmatically (requires knowing which starter/provider you want to tune). Tell me which provider to target (ollama or gemini) and I will implement the ChatModel configuration changes.