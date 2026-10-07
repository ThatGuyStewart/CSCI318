@echo off
echo ===================================================
echo   Stopping Online Retail Microservices
echo ===================================================

for %%P in (8081 8082 8083 8084) do (
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%%P" ^| findstr "LISTENING"') do (
        echo Stopping process on port %%P ^(PID: %%a^)...
        taskkill /F /PID %%a >nul 2>&1
    )
)

echo Done. All 4 services stopped.

echo Attempting to stop Kafka (docker compose)...
docker compose -f "%~dp0kafka-compose.yml" down >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Could not stop Kafka (docker compose failed or Docker not running). Skipping Kafka stop.
) else (
    echo Kafka stopped via docker compose.
)

pause
