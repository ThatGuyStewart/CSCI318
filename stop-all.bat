@echo off
echo ===================================================
echo   Stopping Online Retail Microservices
echo ===================================================

for %%P in (8081 8082 8083 8084) do (
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%%P" ^| findstr "LISTENING"') do (
        echo Stopping process on port %%P (PID: %%a)...
        taskkill /F /PID %%a >nul 2>&1
    )
)

echo Done. All 4 services stopped.
pause
