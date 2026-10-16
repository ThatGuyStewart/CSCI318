runtime configuration

Goal: configure model options (temperature, max_tokens) for langchain4j starter.

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

4) Testing
- Rebuild and start service:
- Run the recommendation curl test repeatedly and inspect `ModelLogger` output for raw responses.
