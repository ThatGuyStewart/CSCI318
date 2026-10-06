Ollama runtime configuration

Goal: configure Ollama model options (temperature, max_tokens) for langchain4j Ollama starter.

1) Environment variables
- Set these environment variables before starting the service (Windows cmd/powershell example):

```powershell
setx AI_MODEL_TEMPERATURE 0.0
setx AI_MODEL_MAX_OUTPUT_TOKENS 512
# or for current session
$env:AI_MODEL_TEMPERATURE = "0.0"
$env:AI_MODEL_MAX_OUTPUT_TOKENS = "512"
```

2) application.properties
Add provider-specific properties if your starter supports them (check starter docs). Example keys used by some starters:

ai.model.temperature=0.0
ai.model.max-output-tokens=512

3) Verify logs
- The `AgentConfiguration` now copies `ai.model.*` properties into system properties. Check application logs for lines like:
  "Set ai.model.temperature=0.0 from environment"

4) If Ollama starter still does not apply those properties automatically
- Consult the starter README for which property names it expects. Map `ai.model.*` to those keys via environment or application.properties.
- As a last resort, modify the ChatModel bean creation in your project to explicitly pass options into the Ollama client constructor (requires reading starter API). 

5) Testing
- Rebuild and start service:

mvn -DskipTests package
java -jar product-service/target/product-service-1.0.0.jar.original

- Run the recommendation curl test repeatedly and inspect `ModelLogger` output for raw responses.

If you want, I can implement explicit Ollama client option wiring in code, but I need the Ollama starter API details or permission to add direct dependency calls.