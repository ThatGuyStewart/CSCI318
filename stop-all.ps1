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
