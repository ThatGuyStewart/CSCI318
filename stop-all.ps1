# PowerShell script to stop all 4 microservices

Write-Host "Stopping all retail microservices on ports 8081-8084..." -ForegroundColor Yellow

$ports = @(8081, 8082, 8083, 8084)
foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($connections) {
        foreach ($conn in $connections) {
            $pidToKill = $conn.OwningProcess
            Write-Host "Killing process on port $port (PID: $pidToKill)..." -ForegroundColor Cyan
            Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
        }
    } else {
        Write-Host "No process listening on port $port." -ForegroundColor Gray
    }
}

Write-Host "Done. All services stopped." -ForegroundColor Green
n# Attempt to stop Kafka (docker compose)
Write-Host "Stopping Kafka (docker compose)..." -ForegroundColor Yellow
$kafkaComposeFile = Join-Path $PSScriptRoot 'kafka-compose.yml'
if (Test-Path $kafkaComposeFile) {
    try {
        & docker compose -f $kafkaComposeFile down *> $null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Kafka stopped via docker compose." -ForegroundColor Green
        } else {
            Write-Host "Docker compose down returned non-zero exit code. Kafka may not have been stopped." -ForegroundColor Red
        }
    } catch {
        Write-Host "Failed to run docker compose. Is Docker running? $_" -ForegroundColor Red
    }
} else {
    Write-Host "Kafka compose file not found at $kafkaComposeFile. Skipping Kafka stop." -ForegroundColor Gray
}
