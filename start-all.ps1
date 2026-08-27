# PowerShell script to boot all 4 microservices

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Starting Online Retail Microservices Platform    " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

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
