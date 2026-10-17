# Technical Architecture

## Microservice Architecture

This application contains four **Microservice Modules**:

* **Customer**
* **Product**
* **Order**
* **Notification**



## Layered Architecture

Each microservice module is structured into four distinct layers:

1. **Presentation Layer** (Controllers)
2. **Service Layer** (Business Logic)
3. **Domain Layer** (Core Domain Models \& Rules)
4. **Data Access Layer** (Repositories)



## Repository Structure

The project is structured as a multi-module repository:

* `customer-service`: Module for the Customer Service
* `product-service`: Module for the Product Service
* `order-service`: Module for the Order Service
* `notification-service`: Module for the Notification Service
* `retail-common`: Shared library for cross-service utilities and contracts; it is not a deployable microservice



## Technology Stack

* **JDK \& Build**: Java 21, Apache Maven (Multi-module POM)
* **Framework**: Spring Boot

  * **Controller Layer**: `@RestController`
  * **Service Layer**: `@Service`
  * **Domain Layer**: `@Entity`
  * **Data Access Layer**: `@Repository`
* **Database**: Each service uses its own file-backed H2 database in the local configuration.
* **Messaging**: Apache Kafka with service-owned event topics, Spring Kafka consumers, and a transactional outbox.
* **Inter-Service Compatibility**: DTOs to be used for communication between services
* **AI-Powered**: LangChain4j supports read-only product recommendations in Product Service.
* **Unit \& Integration Testing**: `MockMvc`, `@SpringBootTest`, `JUnit 5`



## Design Principles

* Services persist mutable command-side entities and append domain-event records in the same database transaction.
* The transactional outbox publishes committed events to Kafka using the aggregate type and ID as the message key.
* Kafka consumers use processed-event records to make cross-service updates idempotent.
* Each service maintains its query projection from its service-owned Kafka topic using Kafka Streams. Database checkpoints keyed by aggregate ID/version tolerate at-least-once delivery.
* The current implementation is **not full event sourcing**: mutable aggregates remain authoritative during command handling. Explicit replay rebuilds each service's read projection from its own persisted event log.
* Notification Service persists notifications and a notification read view; it is not an event-sourced aggregate. `NotificationView` is maintained from `NotificationCreatedEvent`.

## Streaming and Replay Boundaries

* Kafka topics `customer.events`, `product.events`, `order.events`, and `notification.events` are live transports. Each service's persisted event log is its own projection replay source; Kafka retention is not treated as an indefinitely retained archive.
* Projection replay is an explicit maintenance operation. Stop the corresponding service before replay so its read model and version checkpoints can be rebuilt consistently.
* Set `RETAIL_EVENTS_STREAMS_ENABLED=false` to use each service's synchronous in-process projector instead of consuming its topic. This fallback does not consume already-published Kafka events.
* CustomerView replay rebuilds profile and basket-total state from Customer events, then reconciles the current basket total from the mutable Basket table. This preserves correct totals for historical order-driven basket clears that predate the local `BasketClearedAfterOrderEvent`.
* Aggregate reconstruction, snapshots, and making event logs the authoritative write model remain separate future work.



## Future Considerations

* Define a production database and Kafka deployment, event retention, schema evolution, and operational monitoring.
* Evaluate full event sourcing separately from event-driven projection rebuilding.
