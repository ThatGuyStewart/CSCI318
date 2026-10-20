# Online Retail Microservices Platform

A multi-module Spring Boot microservices platform for an online retail system, built as part of **CSCI318 Assignment**.

---

## Quick Start

### Dependencies

Install these before starting the platform:

1. **Java Development Kit (JDK) 21** or later. Verify with `java -version`.
2. **Apache Maven 3.8+**. Verify with `mvn -version`.
3. **Docker Desktop**, which runs the local Kafka broker used for event publishing and projections. Verify with `docker info` after Docker Desktop has started.
4. **Ollama**. Run `ollama pull llama3.2:3b` after installation.

### Start The Platform

1. Open Docker Desktop and sign in. Wait until it reports that the engine is running.
2. Open a terminal in the repository root.
3. Run the platform launcher for your operating system:

Ollama agent configuration:

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

Gemini agent configuration:

    **Windows Command Prompt**
    ```bat
    start-all-gemini.bat
    ```

    **Windows PowerShell**
    ```powershell
    .\start-all-gemini.ps1
    ```

    **macOS / Linux**
    ```bash
    ./start-all-gemini.sh
    ```

4. The launcher starts Kafka at `localhost:9092`, creates the required event and dead-letter topics, installs the shared Maven module, and opens the four services on ports `8081` through `8084`.
5. Seed example data after the services finish starting (first run only, as data persists between runs):
    ```bat
    populate-data.bat
    ```
6. Stop the services with `stop-all.bat` (or the matching `.ps1` or `.sh` script). Stop Kafka when finished:
    ```bash
    docker compose -f kafka-compose.yml down
    ```

7. In Windows PowerShell, use `curl.exe` to avoid the `curl` alias for `Invoke-WebRequest`. Command Prompt and Bash do not have this PowerShell alias.

Windows batch launchers must be saved and committed with CRLF line endings. `.gitattributes` disables Git's text conversion for `.bat` files so their CRLF bytes are preserved in both clones and GitHub **Download ZIP** archives; a checkout-only `eol=crlf` rule does not fix ZIP downloads. If Command Prompt reports `The system cannot find the batch label specified - ensureServicePortsAvailable` in an older download, open `start-all.bat` in VS Code, click the line-ending indicator in the status bar, select **CRLF**, save, and rerun the launcher. The label exists; LF line endings can prevent Command Prompt from finding it. When editing any `.bat` file, keep its line-ending setting at **CRLF**.

The default AI profile is `ollama`. For recommendations, start Ollama and download the configured model with `ollama pull llama3.2:3b`. To run without recommendations, set `AI_RECOMMENDATION_ENABLED=false`. See [AI configuration](#ai-configuration) for Gemini setup.
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

This project implements a distributed **Online Retail Platform** using a microservices architecture. It consists of four independent Spring Boot services that communicate through REST APIs and Kafka domain events to support customer management, product cataloguing, order processing, and customer notifications.

---

## Architecture

The platform follows a **Layered Microservice Architecture**, combining synchronous REST dependencies with asynchronous Kafka event flows.

### REST APIs and Database Ownership

Arrows between services represent REST calls from the caller to the data-owning service. Clients can call all four services directly; Product's Customer and Order lookups are used by its recommendation tools.

```text
                  +-----------------------------------------+
                  |          Client / API Consumer          |
                  +--------------------+--------------------+
                                       |
             +-----------------+-------+---------+-----------------+
             |                 |                 |                 |
             v                 v                 v                 v
    +----------------+ +----------------+ +----------------+ +------------------+
    | Customer Svc   | | Product Svc    | | Order Svc      | | Notification Svc |
    | :8081          | | :8082          | | :8083          | | :8084            |
    +--------+-------+ +--------+-------+ +--------+-------+ +---------+--------+
             |                  |                 |                   |
             v                  v                 v                   v
    +----------------+ +----------------+ +----------------+ +------------------+
    | Customer H2    | | Product H2     | | Order H2       | | Notification H2  |
    +----------------+ +----------------+ +----------------+ +------------------+

    Synchronous REST dependencies (caller --> data-owning service):

    Customer Svc     --> Product Svc       Product validation
    Order Svc        --> Customer Svc      Customer and basket lookup
    Order Svc        --> Product Svc       Product lookup
    Notification Svc --> Customer Svc      Customer lookup
    Product Svc      --> Customer Svc      Recommendation profile lookup
    Product Svc      --> Order Svc         Recommendation order-history lookup
```

Each service owns a separate file-backed H2 database containing its write entities, persisted domain events, read projections, projection checkpoints and outbox records. Customer and Notification also persist processed-event IDs for their cross-service listeners. Services do not access one another's database tables.

### Kafka Events and Read Projections

Each service publishes its own domain events through a transactional outbox and consumes its own topic through Kafka Streams to maintain its database-backed read projection. Additional Spring Kafka listeners handle cross-service reactions.

```text
                         Transactional       Apache Kafka :9092
                         outbox publishing
    +------------------+                   +---------------------+
    | Customer Svc     | ----------------> | customer.events     |
    +------------------+                   +---------------------+
    +------------------+                   +---------------------+
    | Product Svc      | ----------------> | product.events      |
    +------------------+                   +---------------------+
    +------------------+                   +---------------------+
    | Order Svc        | ----------------> | order.events        |
    +------------------+                   +---------------------+
    +------------------+                   +---------------------+
    | Notification Svc | ----------------> | notification.events |
    +------------------+                   +---------------------+

    Own-topic Kafka Streams consumption:

    customer.events     --> Customer Svc     --> CustomerView
    product.events      --> Product Svc      --> ProductView
    order.events        --> Order Svc        --> OrderSummaryView
    notification.events --> Notification Svc --> NotificationView

    Cross-service Spring Kafka listeners:

    product.events --> Customer Svc       Update or remove basket items
    order.events   --> Customer Svc       Clear basket after basket-backed checkout
    order.events   --> Notification Svc   Create order notifications
```

Automatic order notifications are triggered by `OrderPlacedEvent`, `OrderCancelledEvent` and `OrderCancelFailedEvent`, not by a direct Order-to-Notification REST call. Ordinary order-status changes do not currently trigger notifications. Read projections and cross-service reactions are eventually consistent; command responses do not guarantee that these asynchronous updates have finished.

Each microservice is structured into four distinct layers:

| Layer | Annotation | Responsibility |
|---|---|---|
| **Presentation** | `@RestController` | Handles HTTP requests and responses |
| **Service** | `@Service` | Contains business logic |
| **Domain** | `@Entity` | Core domain models and business rules |
| **Data Access** | `@Repository` | Database interaction |

**Inter-service communication** combines synchronous REST calls using DTOs with asynchronous domain events over Kafka. Each service also uses Kafka Streams to maintain its own database-backed read projection.

---

## Technology Stack

| Component | Technology |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.16 |
| **Build Tool** | Apache Maven (Multi-module POM) |
| **Database** | Separate file-backed H2 database per service; in-memory H2 for tests |
| **Persistence** | Spring Data JPA |
| **Messaging and Streaming** | Apache Kafka, Spring Kafka, Kafka Streams; transactional outbox and database-backed projections |
| **AI Recommendations** | LangChain4j; Ollama or Gemini chat model; local AllMiniLmL6V2 embeddings |
| **Validation** | Spring Boot Validation |
| **Testing** | JUnit 5, MockMvc, `@SpringBootTest` |

---

## Project Structure

```
retail-platform/
+-- pom.xml                        # Parent multi-module POM
+-- retail-common/                # Shared events, outbox, DLT routing and observability
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
|   +-- 5-event-sourcing.md
|   +-- 6-cqrs-command-query.md
|   +-- 7-AI-agent.md
|   +-- openapi-contract.yaml
+-- api-examples.txt               # cURL and PowerShell API examples
+-- populate-data.ps1              # PowerShell script to seed test data
+-- populate-data.bat              # Batch script to seed test data
+-- start-all.ps1                  # Start all services (PowerShell)
+-- start-all.bat                  # Start all services (Batch)
+-- start-all.sh                   # Start all services (Bash)
+-- start-all-gemini.bat           # Select Gemini, then run the Batch launcher
+-- start-all-gemini.ps1           # Select Gemini, then run the Batch launcher
+-- start-all-gemini.sh            # Select Gemini, then run the Bash launcher
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
- Looks up product details through Product Service when adding to a basket. Current limitation: a 404/empty product lookup uses a placeholder name and price 10.0 rather than rejecting the item; downstream connectivity failures return a service-unavailable error.

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
- Publishes events that asynchronously trigger notifications on order placement, cancellation and failed cancellation; ordinary status changes do not trigger notifications

### 4. Notification Service — `http://localhost:8084`

Records and retrieves customer notifications.

- Create targeted notifications by customer ID, email, or phone
- Broadcast notifications to all customers or customers in a specific area (postcode, state, or country)
- Query notifications by customer, by date, or by date range
- Records notification messages and their preferred Email/Phone type; external email/SMS delivery is not implemented

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

The start scripts also start Kafka at `localhost:9092` and create the event, projection, and cross-service consumer DLT topics. To stop it after stopping the services, run:

```bash
docker compose -f kafka-compose.yml down
```

Each service exposes `GET /actuator/health`; while Kafka Streams is enabled, the health result reports the stream's state. `GET /actuator/metrics/retail.events.outbox.pending` reports the number of unpublished outbox records. Protect actuator endpoints with authentication and network policy in any non-local deployment.

### AI Configuration

Provider selection uses Spring profiles, not a `-model` launcher argument. The default profile is `ollama`; supported provider settings are:

| Setting | Default / purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `ollama`; set to `gemini` to select Gemini |
| `AI_RECOMMENDATION_ENABLED` | `true`; set to `false` to disable recommendations |
| `OLLAMA_BASE_URL` | `http://localhost:11434` |
| `OLLAMA_MODEL_NAME` | `llama3.2:3b` |
| `GEMINI_API_KEY` | Required for Gemini; supply through the environment |
| `GEMINI_MODEL_NAME` | `gemini-3.5-flash-lite`; override with a tool-capable model available to your key |

For example, in Bash:

```bash
export GEMINI_API_KEY="your-api-key"
bash start-all-gemini.sh
```

On Windows Command Prompt:

```bat
set "GEMINI_API_KEY=your-api-key"
start-all-gemini.bat
```

The wrappers select the Gemini profile and delegate to the existing launcher; they do not supply credentials or download models. Restart existing service processes to load configuration changes.

### Projection Replay and Kafka-Free Operation

Normal startup reuses file-backed H2 data and Kafka offsets. Replay is disabled by default and is an explicit maintenance operation: it rebuilds a service's read projection and version checkpoints from its persisted H2 event log, not from Kafka. It does not recreate all write entities or republish old events. See [replay instructions](specs/5-event-sourcing.md#4-current-implementation-and-replay).

For Kafka-free development, disable `retail.events.streams.enabled`, `retail.events.publishing.enabled`, and `retail.events.outbox.enabled`; also disable `retail.events.consumer.enabled` in Customer and Notification. With streams disabled, commands apply their own projections synchronously, but cross-service event reactions do not run.

The local Kafka Compose configuration has no persistent volume. Removing its container can discard topics and offsets while H2 data remains; do not treat Kafka as the durable projection-replay archive.

### Running a Single Service

First start Kafka and provision its topics as described above, then install the shared module from the repository root with `mvn -pl retail-common -am install -DskipTests`. Run one of:

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
| `GET` | `/product/recommendation?message={message}&sessionId={sessionId}` | Read-only AI recommendations; sessionId is optional |
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
| `GET` | `/order/event` | List all order domain events |
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
| `GET` | `/notification` | List notifications, optionally filtered by date or date range |
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
Coverage also includes all four projection topologies, replay and checkpoint behaviour, replay rollback, listener dead-letter handling, outbox dispatch failures, and embedding deletion/transaction rollback. Recommendation controller tests use a mocked agent; they do not establish live Ollama/Gemini availability or recommendation quality.
The shared module also runs a broker-level Kafka Streams integration test with an embedded Kafka broker; this verifies projection validation DLT routing and malformed-record DLT routing without Docker. Unit tests also cover legacy event envelope schema defaults and Streams health status:

```bash
mvn -pl retail-common -am -Dtest=ProjectionFailureRouterEmbeddedKafkaTest -Dsurefire.failIfNoSpecifiedTests=false test
```

---

## Inter-Service Dependencies

The REST and Kafka dependency diagrams are under [Architecture](#architecture). Order notifications use Kafka, not a direct Order-to-Notification REST call. Product recommendation tools make read-only Customer and Order REST requests when personalisation is needed.

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
    - The repository includes `kafka-compose.yml` (root) that launches a local Kafka broker for development.
    - The broker disables automatic topic creation. The start scripts create `customer.events`, `product.events`, `order.events`, `notification.events`, each service's `*.projection.dlt`, each service's `*.projection.deserialization.dlt`, and `customer.consumer.dlt` / `notification.consumer.dlt` before launching services. When starting services manually, create these topics first or use one of the start scripts.
    - To start Kafka only (manual):

```bash
docker compose -f kafka-compose.yml up -d
```

    - To stop and remove the stack:

```bash
docker compose -f kafka-compose.yml down
```

3) LLM / Agent (optional runtime features)
    - See [AI Configuration](#ai-configuration) for the supported Ollama/Gemini profiles and environment variables. `AI_MODEL_PROVIDER`, `AI_MODEL_API_KEY`, and OpenAI are not wired as provider-selection options.
    - Embeddings use the local AllMiniLmL6V2 model by default; chat-provider selection does not select the embedding model.
    - To disable recommendations, set `AI_RECOMMENDATION_ENABLED=false` or `retail.ai.recommendation.enabled=false`.

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
docker compose -f kafka-compose.yml exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

    - Consume messages for troubleshooting (quick check):

```bash
docker compose -f kafka-compose.yml exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order.events --from-beginning --max-messages 10
```

8) Environment / application properties
    - The services use Spring Boot externalized configuration. Typical mechanisms:
        - `application.properties` / `application.yml` in `src/main/resources`
        - Environment variables (`SPRING_DATASOURCE_URL`, `GEMINI_API_KEY`, etc.)
        - Application arguments such as `java -jar ... --retail.ai.recommendation.enabled=false`
        - JVM system properties before `-jar`, or forwarded by Maven using `-Dspring-boot.run.jvmArguments="..."`

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

10) Troubleshooting
    - If a service won't start, check logs in the service `target` or console where `spring-boot:run` is executed.
    - Common issues:
        - Port already in use: change `server.port` in `application.properties`.
        - Kafka not available: confirm Docker compose stack is running and `localhost:9092` is reachable.
        - LLM/embedding provider errors: ensure provider environment variables are set or disable the agent feature.
        - Invoke-WebRequest errors in Windows PowerShell: use `curl.exe` instead of the `curl` alias.

11) Security & production notes
    - For production use, replace file-backed H2 with a production RDBMS (Postgres, MySQL). Update `spring.datasource.*` accordingly.
    - Secure Kafka with TLS/SASL in production and configure appropriate credentials.
    - Authentication and authorisation are not implemented. Add REST access controls such as OAuth2/JWT before production use.


## AI, Logging & Embeddings (Operational Notes)

- AI recommendation feature: enabled by property `retail.ai.recommendation.enabled` (default true). Set `AI_RECOMMENDATION_ENABLED=false` to disable it.
- Model-content logging is disabled by default to avoid leaking prompts or PII. Two opt-in runtime flags exist:
    - `product.recommendation.logRawReplies` — when `true` enables debug logging of raw model replies emitted by the recommendation controller (default: `false`). Use only for local debugging and never enable in production.
    - `product.recommendation.logModelContent` — when `true` enables debug logging of model request/response content emitted by internal model listeners (default: `false`). Use only for local development.
- The default logging level for the LangChain4j internals (`dev.langchain4j`) has been lowered to `INFO` to avoid inadvertently logging prompts/tool calls. Enable DEBUG explicitly in local/dev environments only, e.g. set `logging.level.dev.langchain4j=DEBUG` in a non-production profile.
- To enable the opt-in debug logging at JVM launch, pass system properties, for example:

```
# To run product-service with raw reply debug logging and model content debug logging (run from project root):
mvn -pl product-service spring-boot:run "-Dspring-boot.run.jvmArguments=-Dproduct.recommendation.logRawReplies=true -Dproduct.recommendation.logModelContent=true -Dlogging.level.com.retail.product=DEBUG"
```

- Embedding store updates: the product service attempts a best-effort removal of any existing embedding entry for the same `productId` before adding a fresh embedding (reflection-based to remain compatible with multiple embedding-store APIs). Embedding/model invocation failures are non-fatal and logged at DEBUG with the exception type (no prompt/content is logged).
- Product deletion removes the matching embedding after the database transaction commits; rolled-back deletions retain it. Removal failures are non-fatal and logged at WARN with the product ID and exception type, without prompt/content.
- Startup ingestion indexes existing `ProductView` records. Create/update embedding text contains name, category and description, not price. These updates run in the command path, not the Kafka projector; projection replay does not itself refresh the in-memory index. Create/update embedding changes are best-effort and are not transactionally coupled to database commit.
- Catalogue validation checks the response's product list against current projections, but does not fully verify generated prose or guarantee privacy. User messages can contain PII and are passed to the chat model and in-memory session history; there is no general pre-model or pre-memory PII-redaction step. Use synthetic data for demos, especially with a remote provider.
- Content logging switches do not cover every log path: some recommendation tool fallbacks log search queries and product names at INFO. Treat application logs as potentially containing user-derived text.
- Tests: new integration tests were added for recommendation behaviour (see `src/test/java/com/retail/product/RecommendationAgentControllerIntegrationTest.java`). The product service tests run against an in-memory H2 database. To run only the product-service tests:

```
mvn -pl product-service clean test
```

Use these notes when debugging AI/model related issues and ensure debug logging and any content logging flags are only enabled in safe, non-production environments.
