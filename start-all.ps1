# PowerShell script to boot all 4 microservices

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Starting Online Retail Microservices Platform    " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

$occupiedPorts = foreach ($port in 8081, 8082, 8083, 8084, 9092) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        $port
    }
}
if ($occupiedPorts) {
    if ($occupiedPorts -contains 9092) {
        Write-Host "Kafka port 9092 is already in use. Stop the existing broker or free the port before starting the platform." -ForegroundColor Red
        exit 1
    }
    Write-Host "Service port(s) already in use: $($occupiedPorts -join ', '). Run .\stop-all.ps1 first." -ForegroundColor Red
    exit 1
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    $dockerDesktop = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"
    if (-not (Test-Path $dockerDesktop)) {
        Write-Host "Docker Desktop was not found. Install Docker Desktop and try again." -ForegroundColor Red
        exit 1
    }

    Write-Host "Starting Docker Desktop..." -ForegroundColor Yellow
    Start-Process -FilePath $dockerDesktop
    $dockerReady = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        Start-Sleep -Seconds 2
        docker info *> $null
        if ($LASTEXITCODE -eq 0) {
            $dockerReady = $true
            break
        }
    }
    if (-not $dockerReady) {
        Write-Host "Docker Desktop did not become ready within one minute." -ForegroundColor Red
        exit 1
    }
}

Write-Host "Starting Kafka on port 9092..." -ForegroundColor Yellow
docker compose -f "$PSScriptRoot/kafka-compose.yml" up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to start Kafka. Ensure Docker Desktop is running." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "Waiting for Kafka to become ready..." -ForegroundColor Yellow
$kafkaReady = $false
for ($attempt = 1; $attempt -le 30; $attempt++) {
    docker compose -f "$PSScriptRoot/kafka-compose.yml" exec -T kafka `
        /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list *> $null
    if ($LASTEXITCODE -eq 0) {
        $kafkaReady = $true
        break
    }
    Start-Sleep -Seconds 2
}
if (-not $kafkaReady) {
    Write-Host "Kafka did not become ready within one minute." -ForegroundColor Red
    exit 1
}

Write-Host "Preparing shared Maven dependencies..." -ForegroundColor Yellow
& mvn.cmd -pl retail-common -am install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to prepare shared Maven dependencies." -ForegroundColor Red
    exit $LASTEXITCODE
}

$services = @(
    @{ Name = "Customer Service"; Module = "customer-service"; Port = 8081 },
    @{ Name = "Product Service";  Module = "product-service";  Port = 8082 },
    @{ Name = "Order Service";    Module = "order-service";    Port = 8083 },
    @{ Name = "Notification Service"; Module = "notification-service"; Port = 8084 }
)

foreach ($s in $services) {
    Write-Host "Starting $($s.Name) on port $($s.Port)..." -ForegroundColor Yellow
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k mvn.cmd -pl $($s.Module) spring-boot:run" -WorkingDirectory $PSScriptRoot
    Start-Sleep -Seconds 3
}

Write-Host "`nAll 4 microservices started in separate windows!" -ForegroundColor Green
Write-Host "  Customer Service:     http://localhost:8081" -ForegroundColor White
Write-Host "  Product Service:      http://localhost:8082" -ForegroundColor White
Write-Host "  Order Service:        http://localhost:8083" -ForegroundColor White
Write-Host "  Notification Service: http://localhost:8084" -ForegroundColor White
Write-Host "`nRun .\stop-all.ps1 to stop all running microservices." -ForegroundColor Gray
