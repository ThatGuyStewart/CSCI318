# Online Retail Microservices Platform

A multi-module Spring Boot microservices platform for an online retail system, built as part of **CSCI318 Assignment**.

---

## Quick Start

### Dependencies

Install these before starting the platform:

1. **Java Development Kit (JDK) 21** or later. Verify with `java -version`.
2. **Apache Maven 3.8+**. Verify with `mvn -version`.
3. **Docker Desktop**, which runs the local Kafka broker used for event publishing and projections. Verify with `docker info` after Docker Desktop has started.

### Start The Platform

1. Open Docker Desktop and sign in. Wait until it reports that the engine is running.
2. Open a terminal in the repository root.
3. Run the platform launcher for your operating system:

    **Windows Command Prompt**
    ```bat
    start-all.bat
    ```

    **Windows PowerShell**
    ```powershell
    .\start-all.ps1
    ```

    **macOS / Linux**
    ```bash
    ./start-all.sh
    ```

4. The launcher starts Kafka at `localhost:9092`, installs the shared Maven module, and opens the four services on ports `8081` through `8084`.
5. Seed example data after the services finish starting (first run only, as data persists between runs):
    ```bat
    populate-data.bat
    ```
6. Stop the services with `stop-all.bat` (or the matching `.ps1` or `.sh` script). Stop Kafka when finished:
    ```bash
    docker compose -f kafka-compose.yml down
    ```

7. If curl commands fail with an Invoke-WebRequest error, run "Remove-item alias:curl" in the command prompt to remove the Invoke-WebRequest CmdLet's curl alias.
---

## Table of Contents

- [Quick Start](#quick-start)
- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Services](#services)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Testing](#testing)

---

## Overview

This project implements a distributed **Online Retail Platform** using a microservices architecture. It consists of four independent Spring Boot services that communicate with each other via REST APIs to support customer management, product cataloguing, order processing, and customer notifications.

---

## Architecture

The platform follows a **Layered Microservice Architecture**:

```
+---------------------------------------------------------+
|                  Client / API Consumer                  |
+-------------------+-------------------+-----------------+
                    |                   |
           +--------+------+   +--------+------+   +--------------+
           | Customer Svc  |   | Product Svc   |   |  Order Svc   |
           |   :8081       |   |   :8082       |   |  :8083       |
           +---------------+   +---------------+   +------+-------+
                                                          |
                                                 +--------+----------+
                                                 | Notification Svc  |
                                                 |     :8084         |
                                                 +-------------------+
```

Each microservice is structured into four distinct layers:

| Layer | Annotation | Responsibility |
|---|---|---|
| **Presentation** | `@RestController` | Handles HTTP requests and responses |
| **Service** | `@Service` | Contains business logic |
| **Domain** | `@Entity` | Core domain models and business rules |
| **Data Access** | `@Repository` | Database interaction |

**Inter-service communication** is handled via REST calls using DTOs to maintain loose coupling.

---

## Technology Stack

| Component | Technology |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.16 |
| **Build Tool** | Apache Maven (Multi-module POM) |
| **Database** | H2 (in-memory, for development) |
| **Persistence** | Spring Data JPA |
| **Validation** | Spring Boot Validation |
| **Testing** | JUnit 5, MockMvc, `@SpringBootTest` |

---

## Project Structure

```
retail-platform/
+-- pom.xml                        # Parent multi-module POM
+-- customer-service/              # Customer & Basket management (port 8081)
|   +-- src/main/java/com/retail/customer/
|       +-- controller/
|       +-- service/
|       +-- domain/
|       +-- repository/
|       +-- dto/
|       +-- client/
|       +-- exception/
+-- product-service/               # Product catalogue (port 8082)
|   +-- src/main/java/com/retail/product/
|       +-- controller/
|       +-- service/
|       +-- domain/
|       +-- repository/
|       +-- dto/
|       +-- exception/
+-- order-service/                 # Order processing (port 8083)
|   +-- src/main/java/com/retail/order/
|       +-- controller/
|       +-- service/
|       +-- domain/
|       +-- repository/
|       +-- dto/
|       +-- client/
|       +-- exception/
+-- notification-service/          # Customer notifications (port 8084)
|   +-- src/main/java/com/retail/notification/
|       +-- controller/
|       +-- service/
|       +-- domain/
|       +-- repository/
|       +-- dto/
|       +-- client/
|       +-- exception/
+-- specs/                         # Project specifications
|   +-- 1-technical-architecture.md
|   +-- 2-user-stories.md
|   +-- 3-domain-models.md
|   +-- 4-api-endpoints.md
|   +-- openapi-contract.yaml
+-- api-examples.txt               # cURL and PowerShell API examples
+-- populate-data.ps1              # PowerShell script to seed test data
+-- populate-data.bat              # Batch script to seed test data
+-- start-all.ps1                  # Start all services (PowerShell)
+-- start-all.bat                  # Start all services (Batch)
+-- start-all.sh                   # Start all services (Bash)
+-- stop-all.ps1                   # Stop all services (PowerShell)
+-- stop-all.bat                   # Stop all services (Batch)
+-- stop-all.sh                    # Stop all services (Bash)
```

---

## Services

### 1. Customer Service — `http://localhost:8081`

Manages customer records and shopping baskets.

- Create, read, update, and delete customer records
- Lookup customers by ID, email, or phone
- View and manage a customer's shopping basket
- Validates product existence via inter-service call to Product Service when adding to basket

### 2. Product Service — `http://localhost:8082`

Manages the product catalogue.

- Create, update, and delete products
- List all products or filter by category
- Search products by name (partial match)
- Retrieve a single product by ID

### 3. Order Service — `http://localhost:8083`

Handles order creation, tracking, and lifecycle management.

- Create orders from a customer's basket or from an explicit product + quantity
- Cancel orders in `Placed` or `Pending` status
- Update order status
- Query orders by customer (ID, email, phone) or by product
- Automatically triggers notifications via Notification Service on order create/cancel

### 4. Notification Service — `http://localhost:8084`

Records and retrieves customer notifications.

- Create targeted notifications by customer ID, email, or phone
- Broadcast notifications to all customers or customers in a specific area (postcode, state, or country)
- Query notifications by customer, by date, or by date range

---

## Getting Started

### Prerequisites

- **Java 21** or later
- **Apache Maven 3.8+**
- **Docker Desktop** (runs the local Kafka broker required for event publishing and projections)

### Running the Services

**Windows (PowerShell):**
```powershell
.\start-all.ps1
```

**Windows (Command Prompt):**
```bat
start-all.bat
```

**macOS / Linux:**
```bash
./start-all.sh
```

Once started, the services are accessible at:

| Service | URL |
|---|---|
| Customer Service | http://localhost:8081 |
| Product Service | http://localhost:8082 |
| Order Service | http://localhost:8083 |
| Notification Service | http://localhost:8084 |

The start scripts also start Kafka at `localhost:9092`. To stop it after stopping the services, run:

```bash
docker compose -f kafka-compose.yml down
```

### Running a Single Service

```bash
mvn -pl customer-service spring-boot:run
mvn -pl product-service spring-boot:run
mvn -pl order-service spring-boot:run
mvn -pl notification-service spring-boot:run
```

### Seeding Test Data

On the first run, after all services are running, populate sample data using:

```powershell
# PowerShell
.\populate-data.ps1
```

```bat
REM Batch
populate-data.bat
```

### Stopping the Services

**Windows (PowerShell):**
```powershell
.\stop-all.ps1
```

**Windows (Command Prompt):**
```bat
stop-all.bat
```

**macOS / Linux:**
```bash
./stop-all.sh
```

---

## API Reference

A full API reference is available in [`specs/4-api-endpoints.md`](specs/4-api-endpoints.md).  
The complete OpenAPI contract is in [`specs/openapi-contract.yaml`](specs/openapi-contract.yaml).  
Practical cURL and PowerShell examples for every endpoint are in [`api-examples.txt`](api-examples.txt).

### Customer Service (`http://localhost:8081`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/customer` | Create a new customer |
| `GET` | `/customer` | List all customers |
| `GET` | `/customer/{id}` | Get customer by ID |
| `GET` | `/customer/email/{email}` | Get customer by email |
| `GET` | `/customer/phone/{phone}` | Get customer by phone |
| `GET` | `/customer/state/{state}` | List customers by state |
| `GET` | `/customer/country/{country}` | List customers by country |
| `GET` | `/customer/postcode/{postcode}` | List customers by postcode |
| `PUT` | `/customer/{id}` | Update customer details |
| `PUT` | `/customer/{id}/address` | Update customer address |
| `DELETE` | `/customer/{id}` | Delete a customer |
| `GET` | `/customer/{id}/basket` | View customer basket |
| `POST` | `/customer/{id}/basket` | Add item to basket |
| `DELETE` | `/customer/{id}/basket` | Remove item from basket |
| `GET` | `/customer/event` | List all customer domain events |
| `GET` | `/customer/{id}/event` | Customer events by ID |
| `GET` | `/customer/email/{email}/event` | Customer events by email |
| `GET` | `/customer/phone/{phone}/event` | Customer events by phone |

### Product Service (`http://localhost:8082`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/product` | Create a new product |
| `PUT` | `/product/{id}` | Update a product |
| `DELETE` | `/product/{id}` | Delete a product |
| `GET` | `/product` | List all products |
| `GET` | `/product/{id}` | Get product by ID |
| `GET` | `/product/category/{category}` | List products by category |
| `GET` | `/product/search?name={name}` | Search products by name |
| `GET` | `/product/event` | List all product domain events |
| `GET` | `/product/{id}/event` | Product events by ID |
| `GET` | `/product/category/{category}/event` | Product events by category |

### Order Service (`http://localhost:8083`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/order` | Create an order |
| `POST` | `/order/{id}/cancel` | Cancel an order |
| `GET` | `/order/{id}` | Get order by ID |
| `GET` | `/order/customer/{customerId}` | Get orders by customer ID |
| `GET` | `/order/customer/email/{email}` | Get orders by customer email |
| `GET` | `/order/customer/phone/{phone}` | Get orders by customer phone |
| `GET` | `/order/product/{productId}` | Get orders containing a product |
| `PUT` | `/order/{id}/status` | Update order status |
| `GET` | `/order/customer/{customerId}/event` | Order events by customer ID |
| `GET` | `/order/{id}/event` | Order events by order ID |
| `GET` | `/order/customer/email/{email}/event` | Order events by customer email |
| `GET` | `/order/customer/phone/{phone}/event` | Order events by customer phone |
| `GET` | `/order/product/{productId}/event` | Order events by product |

### Notification Service (`http://localhost:8084`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/notification/customer/{customerId}` | Create notification by customer ID |
| `POST` | `/notification/customer/email/{email}` | Create notification by email |
| `POST` | `/notification/customer/phone/{phone}` | Create notification by phone |
| `POST` | `/notification/broadcast` | Broadcast to all customers |
| `POST` | `/notification/broadcast/area` | Broadcast to customers in an area |
| `GET` | `/notification/customer/{customerId}` | Get notifications by customer ID |
| `GET` | `/notification/customer/email/{email}` | Get notifications by customer email |
| `GET` | `/notification/customer/phone/{phone}` | Get notifications by customer phone |
| `GET` | `/notification/date/{date}` | Get notifications by date |
| `GET` | `/notification/date-range?from={from}&to={to}` | Get notifications in a date range |
| `GET` | `/notification/{id}` | Get notification by ID |

---

## Testing

Run all tests from the project root:

```bash
mvn clean test
```

Run tests for a specific module:

```bash
mvn -pl customer-service clean test
mvn -pl product-service clean test
mvn -pl order-service clean test
mvn -pl notification-service clean test
```

Tests use `MockMvc` and `@SpringBootTest` with an in-memory H2 database, covering both happy-path and error scenarios (HTTP 400, 404, etc.) derived from the OpenAPI contract.

---

## Inter-Service Dependencies

## Complete Setup & Dependencies (Detailed)

This section lists every dependency, installation steps, environment variables, and verification commands required to run the project end-to-end on a developer machine.

1) System prerequisites
    - Git (optional but recommended): install from https://git-scm.com/
    - Java Development Kit (JDK) 21 or later
        - Install OpenJDK 21 (Adoptium/Temurin or your provider of choice)
        - Verify: `java -version` should print a version >= 21
    - Apache Maven 3.8+ (CLI)
        - Install from https://maven.apache.org/
        - Verify: `mvn -version`
    - Docker Desktop (Windows/macOS) or Docker Engine + Docker Compose (Linux)
        - Required to run Kafka and other infra used by the start scripts
        - Verify: `docker info` and `docker compose version`

2) Kafka and infrastructure
    - The repository includes `kafka-compose.yml` (root) that launches a local Kafka, Zookeeper, and any required connectors for development.
    - To start Kafka only (manual):

```bash
docker compose -f kafka-compose.yml up -d
```

    - To stop and remove the stack:

```bash
docker compose -f kafka-compose.yml down
```

3) LLM / Agent (optional runtime features)
    - The project supports an agentic recommendation component that can use local or remote LLMs/embedding models.
    - Environment variables (examples):
        - `AI_MODEL_PROVIDER` = `gemini` | `ollama` | `openai` | `local` (provider-specific)
        - `AI_MODEL_API_KEY` = provider API key (if required)
        - `ai.model.temperature` = e.g. `0.0`–`1.0` (defaults can be set in application properties)
        - `ai.model.max-output-tokens` = integer
    - If you do not want agent features, disable them in the Product Service application properties:
        - `retail.ai.recommendation.enabled=false`

4) Build and run (full project)
    - From project root, build everything with Maven (parallel threads recommended):

```bash
mvn -T 1C clean package
```

    - Start infra (Kafka) first, then start services using the provided scripts:

Windows (PowerShell)
```powershell
docker compose -f kafka-compose.yml up -d
.\start-all.ps1
```

Windows (Command Prompt)
```bat
docker compose -f kafka-compose.yml up -d
start-all.bat
```

macOS / Linux
```bash
docker compose -f kafka-compose.yml up -d
./start-all.sh
```

5) Run a single service locally (useful for debugging)
    - Example: run only `product-service` in the current shell (hot reload with `spring-boot:run`):

```bash
cd product-service
mvn spring-boot:run
```

6) Seed test data
    - Use the provided scripts after services are started. These scripts perform REST calls to populate sample customers, products and orders.

Windows (PowerShell)
```powershell
.\populate-data.ps1
```

Windows (Command Prompt)
```bat
populate-data.bat
```

7) Verify Kafka topics and messages
    - Show topics (requires Kafka CLI or kafkacat / kafka-topics container):

```bash
docker exec -it <kafka_container_name> kafka-topics --bootstrap-server localhost:9092 --list
```

    - Consume messages for troubleshooting (quick check):

```bash
docker exec -it <kafka_container_name> kafka-console-consumer --bootstrap-server localhost:9092 --topic orders --from-beginning --max-messages 10
```

8) Environment / application properties
    - The services use Spring Boot externalized configuration. Typical mechanisms:
        - `application.properties` / `application.yml` in `src/main/resources`
        - Environment variables (`SPRING_DATASOURCE_URL`, `AI_MODEL_API_KEY`, etc.)
        - Command-line system properties (`-Dai.model.temperature=0.2`)

    - Common properties you may set:
        - `spring.datasource.url` (JDBC if using external DB)
        - `spring.kafka.bootstrap-servers=localhost:9092`
        - `retail.ai.recommendation.enabled=true|false`

9) Running tests
    - Run everything:

```bash
mvn clean test
```

    - Run module-specific tests:

```bash
mvn -pl product-service test
```

10) Export report to PDF (optional)
    - If you have `pandoc` installed and on PATH, convert the generated `CSCI318-report.md` to PDF:

```bash
pandoc CSCI318-report.md -o CSCI318-report.pdf --from markdown+yaml_metadata_block -V geometry:margin=1in
```

11) Troubleshooting
    - If a service won't start, check logs in the service `target` or console where `spring-boot:run` is executed.
    - Common issues:
        - Port already in use: change `server.port` in `application.properties`.
        - Kafka not available: confirm Docker compose stack is running and `localhost:9092` is reachable.
        - LLM/embedding provider errors: ensure provider environment variables are set or disable the agent feature.
	    - Invoke-WebRequest errors when running curl commands: Run "Remove-item alias:curl" when in Powershell to remove the Invoke-WebRequest CmdLet's curl alias.

12) Security & production notes
    - For production use, replace in-memory H2 with a production rdbms (Postgres, MySQL). Update `spring.datasource.*` accordingly.
    - Secure Kafka with TLS/SASL in production and configure appropriate credentials.
    - Secure REST endpoints with OAuth2/JWT (currently not enabled by default for dev).


## AI, Logging & Embeddings (Operational Notes)

- AI recommendation feature: enabled by property `retail.ai.recommendation.enabled` (default true in development). To disable AI recommendations set the property to `false` (for example in `application.properties` or via an environment-specific profile).
- Model-content logging is disabled by default to avoid leaking prompts or PII. Two opt-in runtime flags exist:
    - `product.recommendation.logRawReplies` — when `true` enables debug logging of raw model replies emitted by the recommendation controller (default: `false`). Use only for local debugging and never enable in production.
    - `product.recommendation.logModelContent` — when `true` enables debug logging of model request/response content emitted by internal model listeners (default: `false`). Use only for local development.
- The default logging level for the LangChain4j internals (`dev.langchain4j`) has been lowered to `INFO` to avoid inadvertently logging prompts/tool calls. Enable DEBUG explicitly in local/dev environments only, e.g. set `logging.level.dev.langchain4j=DEBUG` in a non-production profile.
- To enable the opt-in debug logging at JVM launch, pass system properties, for example:

```
# To run product-service with raw reply debug logging and model content debug logging (run from project root):
mvn -pl product-service spring-boot:run -Dspring-boot.run.jvmArguments="-Dproduct.recommendation.logRawReplies=true -Dproduct.recommendation.logModelContent=true -Dlogging.level.com.retail.product=DEBUG"
```

- Embedding store updates: the product service attempts a best-effort removal of any existing embedding entry for the same `productId` before adding a fresh embedding (reflection-based to remain compatible with multiple embedding-store APIs). Embedding/model invocation failures are non-fatal and logged at DEBUG with the exception type (no prompt/content is logged).
- Tests: new integration tests were added for recommendation behaviour (see `src/test/java/com/retail/product/RecommendationAgentControllerIntegrationTest.java`). The product service tests run against an in-memory H2 database. To run only the product-service tests:

```
mvn -pl product-service clean test
```

Use these notes when debugging AI/model related issues and ensure debug logging and any content logging flags are only enabled in safe, non-production environments.

```
Order Service       ────► Customer Service
Order Service       ────► Product Service
Order Service       ────► Notification Service
Customer Service    ────► Product Service  (basket item validation)
Notification Svc    ────► Customer Service
```
