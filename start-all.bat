@echo off
echo ===================================================
echo   Starting Online Retail Microservices Platform
echo ===================================================
echo.

echo [1/4] Starting Customer Service on port 8081...
start "Customer Service (Port 8081)" cmd /k "mvn.cmd -pl customer-service spring-boot:run"

timeout /t 3 /nobreak >nul

echo [2/4] Starting Product Service on port 8082...
start "Product Service (Port 8082)" cmd /k "mvn.cmd -pl product-service spring-boot:run"

timeout /t 3 /nobreak >nul

echo [3/4] Starting Order Service on port 8083...
start "Order Service (Port 8083)" cmd /k "mvn.cmd -pl order-service spring-boot:run"

timeout /t 3 /nobreak >nul

echo [4/4] Starting Notification Service on port 8084...
start "Notification Service (Port 8084)" cmd /k "mvn.cmd -pl notification-service spring-boot:run"

echo.
echo ===================================================
echo   All 4 microservices are booting in separate windows!
echo   Customer Service:     http://localhost:8081
echo   Product Service:      http://localhost:8082
echo   Order Service:        http://localhost:8083
echo   Notification Service: http://localhost:8084
echo ===================================================
echo To stop all services, run stop-all.bat
pause
