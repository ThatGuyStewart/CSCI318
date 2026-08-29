@echo off
echo ===================================================
echo   Starting Online Retail Microservices Platform
echo ===================================================
echo.

call :ensureServicePortsAvailable
if errorlevel 1 (
	pause
	exit /b 1
)

call :ensureDocker
if errorlevel 1 (
	pause
	exit /b 1
)

echo Starting Kafka on port 9092...
docker compose -f "%~dp0kafka-compose.yml" up -d
if errorlevel 1 (
	echo Failed to start Kafka. Ensure Docker Desktop is running.
	pause
	exit /b 1
)

call :waitForKafka
if errorlevel 1 (
	pause
	exit /b 1
)

echo Preparing shared Maven dependencies...
call mvn.cmd -pl retail-common -am install -DskipTests
if errorlevel 1 (
	echo Failed to prepare shared Maven dependencies.
	pause
	exit /b 1
)

echo [1/4] Starting Customer Service on port 8081...
start "Customer Service (Port 8081)" /D "%~dp0" cmd.exe /k "mvn.cmd -pl customer-service spring-boot:run"

timeout /t 3 /nobreak >nul

echo [2/4] Starting Product Service on port 8082...
start "Product Service (Port 8082)" /D "%~dp0" cmd.exe /k "mvn.cmd -pl product-service spring-boot:run"

timeout /t 3 /nobreak >nul

echo [3/4] Starting Order Service on port 8083...
start "Order Service (Port 8083)" /D "%~dp0" cmd.exe /k "mvn.cmd -pl order-service spring-boot:run"

timeout /t 3 /nobreak >nul

echo [4/4] Starting Notification Service on port 8084...
start "Notification Service (Port 8084)" /D "%~dp0" cmd.exe /k "mvn.cmd -pl notification-service spring-boot:run"

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

goto :eof

:ensureDocker
docker info >nul 2>&1
if not errorlevel 1 exit /b 0

echo Starting Docker Desktop...
if not exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
	echo Docker Desktop was not found. Install Docker Desktop and try again.
	exit /b 1
)

start "Docker Desktop" "%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
set /a dockerAttempts=0
:waitForDocker
timeout /t 2 /nobreak >nul
docker info >nul 2>&1
if not errorlevel 1 exit /b 0
set /a dockerAttempts+=1
if %dockerAttempts% GEQ 30 (
	echo Docker Desktop did not become ready within one minute.
	exit /b 1
)
goto waitForDocker

:waitForKafka
echo Waiting for Kafka to become ready...
set /a kafkaAttempts=0
:waitForKafkaLoop
docker compose -f "%~dp0kafka-compose.yml" exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list >nul 2>&1
if not errorlevel 1 exit /b 0
set /a kafkaAttempts+=1
if %kafkaAttempts% GEQ 30 (
	echo Kafka did not become ready within one minute.
	exit /b 1
)
timeout /t 2 /nobreak >nul
goto waitForKafkaLoop

:ensureServicePortsAvailable
for %%P in (8081 8082 8083 8084 9092) do (
	netstat -aon | findstr ":%%P " | findstr "LISTENING" >nul
	if not errorlevel 1 (
		if "%%P"=="9092" (
			echo Kafka port 9092 is already in use. Stop the existing broker or free the port before starting the platform.
			exit /b 1
		)
		echo Port %%P is already in use. Run stop-all.bat before starting the platform.
		exit /b 1
	)
)
exit /b 0
