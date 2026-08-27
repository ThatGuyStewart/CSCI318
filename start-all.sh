#!/usr/bin/env bash

echo "==================================================="
echo "  Starting Online Retail Microservices Platform"
echo "==================================================="

mvn -pl customer-service spring-boot:run &
PID_CUST=$!
echo "Customer Service starting (PID: $PID_CUST) on port 8081"
sleep 2

mvn -pl product-service spring-boot:run &
PID_PROD=$!
echo "Product Service starting (PID: $PID_PROD) on port 8082"
sleep 2

mvn -pl order-service spring-boot:run &
PID_ORDER=$!
echo "Order Service starting (PID: $PID_ORDER) on port 8083"
sleep 2

mvn -pl notification-service spring-boot:run &
PID_NOTIF=$!
echo "Notification Service starting (PID: $PID_NOTIF) on port 8084"

echo ""
echo "All services booting in background."
echo "Press Ctrl+C to exit launcher (services will keep running until stop-all.sh is executed)."
wait
