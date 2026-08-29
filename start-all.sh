#!/usr/bin/env bash

echo "==================================================="
echo "  Starting Online Retail Microservices Platform"
echo "==================================================="

is_port_in_use() {
	local port="$1"
	if command -v lsof >/dev/null 2>&1; then
		lsof -iTCP:"$port" -sTCP:LISTEN -t >/dev/null 2>&1
	elif command -v ss >/dev/null 2>&1; then
		ss -ltnH "sport = :$port" 2>/dev/null | grep -q .
	elif command -v netstat >/dev/null 2>&1; then
		netstat -ltn 2>/dev/null | awk -v port=":$port" '$4 ~ (port "$") { found = 1 } END { exit !found }'
	else
		echo "Cannot check ports: install lsof, ss, or netstat."
		return 2
	fi
}

wait_for_kafka() {
	local compose_file="$1"
	local attempt=1
	while [ "$attempt" -le 30 ]; do
		if docker compose -f "$compose_file" exec -T kafka \
			/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list >/dev/null 2>&1; then
			return 0
		fi
		sleep 2
		attempt=$((attempt + 1))
	done

	echo "Kafka did not become ready within one minute."
	return 1
}

for port in 8081 8082 8083 8084 9092; do
	is_port_in_use "$port"
	port_check_status=$?
	if [ "$port_check_status" -eq 0 ]; then
		if [ "$port" -eq 9092 ]; then
			echo "Kafka port 9092 is already in use. Stop the existing broker or free the port before starting the platform."
			exit 1
		fi
		echo "Port $port is already in use. Run ./stop-all.sh before starting the platform."
		exit 1
	elif [ "$port_check_status" -eq 2 ]; then
		exit 1
	fi
done

if ! docker info >/dev/null 2>&1; then
	echo "Docker is not running. Start Docker Desktop (or the Docker daemon) and try again."
	exit 1
fi

echo "Starting Kafka on port 9092..."
KAFKA_COMPOSE_FILE="$(dirname "$0")/kafka-compose.yml"
docker compose -f "$KAFKA_COMPOSE_FILE" up -d || exit 1
echo "Waiting for Kafka to become ready..."
wait_for_kafka "$KAFKA_COMPOSE_FILE" || exit 1

echo "Preparing shared Maven dependencies..."
mvn -pl retail-common -am install -DskipTests || exit 1

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
