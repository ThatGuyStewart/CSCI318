# Event Sourcing Specification

## 

## 1\. Scope

* Customer, Product, and Order services persist domain events beside mutable write entities.
* Notification Service uses CRUD write storage, persists a `NotificationCreatedEvent`, and also publishes that event.
* Each service consumes its own event topic with Kafka Streams to maintain its read projection.

\---

## 

## 2\. Goals

* Preserve state transitions as immutable events.
* Support queries via a projected read model.
* Provide a replayable history for auditing and analysis.

\---

## 

## 3\. Event Model

### 

### 3.1 Event Fields

|Field|Type|Description|
|-|-|-|
|`eventId`|UUID|Unique event identity. Used for idempotency deduplication.|
|`aggregateId`|Long|Identity of the aggregate instance|
|`aggregateType`|String|Aggregate which created the event (`Customer`, `Product`, `Order`, `Notification`)|
|`eventType`|String|One of the event types listed in section 3.3|
|`aggregateVersion`|Long|Monotonically increasing version of the aggregate stream after this event|
|`occurredAt`|timestamp|Wall-clock time the event was produced|
|`correlationId`|UUID, nullable|Links a request/workflow when supplied|
|`causationId`|UUID, nullable|Event or command that caused this event, when supplied|
|`payload`|JSON|Event-specific data (state change fields)|

### 

### 3.2 Event Stream Identity

* Stream key = `aggregateType` + `aggregateId`

### 

### 3.3 Event Types

|Owner: Aggregate, Entity/Value Object|Event Type|Triggering Behavior|Key Payload Fields|
|-|-|-|-|
|`Customer`, `Customer`|`CustomerCreatedEvent`|Creation of `Customer` record|`customerId`, `name`, `email`, `phone`, `contactMethod`, `address`|
|`Customer`, `Customer`|`CustomerUpdatedEvent`|Update a `Customer` record|`customerId`, Any updated fields (`name`, `email`, `phone`, `contactMethod`, `address`)|
|`Customer`, `Customer`|`CustomerDeletedEvent`|Delete a `Customer` record|`customerId`|
|`Customer`, `Basket`|`BasketItemAddedEvent`|Item added to `Customer` `Basket`|`customerId`, `items<`productId`, item(Any updated fields (`name`, `price`, `quantity`, `subtotal`))>, `total`|
|`Customer`, `Basket`|`BasketItemRemovedEvent`|Item removed from `Customer` `Basket`|`customerId`, `items<`productId`, item(`quantity`, `subtotal`)>, `total`|
|`Customer`, `Basket`|`BasketRecalculatedEvent`|`ProductUpdatedEvent` (payload contains name and/or price), `ProductDeletedEvent`|`customerId`, `items<`productId`, item(Any updated fields (`name`, `price`, `quantity`, `subtotal`))>, `total`|
|`Customer`, `Basket`|`BasketClearedEvent`|Explicitly clear a non-empty basket|`customerId`, `items` (empty), `total` (zero)|
|`Customer`, `Basket`|`BasketClearedAfterOrderEvent`|Customer Service consumes `BasketUsedForOrderEvent` and clears the basket|`customerId`, `items` (empty), `total` (zero); `causationId` is the Order event ID|
|`Product`, `Product`|`ProductCreatedEvent`|Creation of `Product` record|`productId`, `name`, `category`, `price`, `description`|
|`Product`, `Product`|`ProductUpdatedEvent`|Update a `Product` record|`productId`, Any updated fields (`name`, `category`, `price`, `description`)|
|`Product`, `Product`|`ProductDeletedEvent`|Delete a `Product` record|`productId`|
|`Order`|`OrderPlacedEvent`|Creation of `Order` record|`orderId`, `customerId`, `address`, `items`, `total`, `status`|
|`Order`|`BasketUsedForOrderEvent`|A created `Order` uses a `Customer` `Basket` is used for `items` contents|Full Order snapshot (`orderId`, `customerId`, `address`, `items`, `total`, `status`)|
|`Order`|`OrderStatusChangedEvent`|The `status` of an `Order` is updated|Full Order snapshot (`orderId`, `customerId`, `address`, `items`, `total`, `status`)|
|`Order`|`OrderCancelledEvent`|An `Order` is cancelled|Full Order snapshot (`orderId`, `customerId`, `address`, `items`, `total`, `status`)|
|`Order`|`OrderCancelFailedEvent`|An `Order` cancellation cannot be completed|Full Order snapshot (`orderId`, `customerId`, `address`, `items`, `total`, `status`)|
|`Notification`|`NotificationCreatedEvent`|A notification is created|`notificationId`, `customerId`, `type`, `message`, `sentAt`|

\---

## 4. Current Implementation and Replay

Persisted events accompany mutable entity writes; the event logs are not yet the authoritative aggregate stores. Services use Kafka Streams application IDs `customer-projection-v1`, `product-projection-v1`, `order-projection-v1`, and `notification-projection-v1` to apply their own event topics to `CustomerView`, `ProductView`, `OrderSummaryView`, and `NotificationView`. Events are keyed by aggregate type and ID; processing is at-least-once, and database checkpoints by aggregate ID/version make retries and duplicate delivery idempotent.

Domain event envelopes carry `schemaVersion` 1. Older persisted events and Kafka JSON records without the field default to version 1 for compatibility. Projections reject unknown versions into the validation DLT. This is an explicit compatibility boundary, not a schema registry or automatic event upcaster; any future payload evolution must define supported versions and transformations before rollout.

For an existing installation, rebuild each projection before first enabling its Kafka Streams topology so the view and version checkpoints are initialized from the database event log. Stop the corresponding service before replay. Replay clears only that service's projection and checkpoints, then applies persisted events in aggregate-version order within one database transaction. If a record is invalid, the transaction fails and the prior projection remains intact. Customer replay also reconciles basket totals from the current mutable Basket table to cover historical order-driven basket clears that predate the local event.

During live Kafka Streams processing, projection validation failures (`InvalidProjectionEventException`) are written to the service's configured projection dead-letter topic (`customer.projection.dlt`, `product.projection.dlt`, `order.projection.dlt`, or `notification.projection.dlt`). The envelope contains the original event, exception type/message, and failure timestamp. JSON deserialization failures are written to a separate service-specific deserialization DLT; its envelope preserves source topic/partition/offset and the original key/value bytes as Base64. The handler commits past a malformed record only after the DLT write succeeds; a failed DLT write fails the stream. Other failures, including database errors, propagate for Kafka Streams failure handling.

Cross-service Spring Kafka listeners use `customer.consumer.dlt` and `notification.consumer.dlt` for unrecoverable deserialization and unsupported-schema events. Normal processing failures retry indefinitely rather than being silently acknowledged. DLT payloads can contain PII, so production topic ACLs, encryption, and retention must be configured. This live-stream handling does not change offline replay behavior: invalid replay records still fail the replay transaction.

The local start scripts explicitly create the four event topics and ten DLT topics because automatic Kafka topic creation is disabled in the local broker configuration. Production deployments must provision topics with deployment-appropriate partition counts, replication factors, retention, and access controls.

For example, rebuild `ProductView` from the repository root:

```powershell
mvn -pl product-service -am -DskipTests package
Set-Location .\product-service
java -jar target\product-service-1.0.0.jar --retail.projection.replay.enabled=true --retail.events.streams.enabled=false --retail.events.publishing.enabled=false --retail.events.outbox.enabled=false --spring.main.web-application-type=none --retail.ai.recommendation.enabled=false
```

Rebuild `OrderSummaryView` from the repository root using:

```powershell
mvn -pl order-service -am -DskipTests package
Set-Location .\order-service
java -jar target\order-service-1.0.0.jar --retail.projection.replay.enabled=true --retail.events.streams.enabled=false --retail.events.publishing.enabled=false --retail.events.outbox.enabled=false --spring.main.web-application-type=none
```

Customer Service also disables its Kafka listeners for offline replay:

```powershell
mvn -pl customer-service -am -DskipTests package
Set-Location .\customer-service
java -jar target\customer-service-1.0.0.jar --retail.projection.replay.enabled=true --retail.events.streams.enabled=false --retail.events.consumer.enabled=false --retail.events.publishing.enabled=false --retail.events.outbox.enabled=false --spring.main.web-application-type=none
```

Notification Service can similarly rebuild `NotificationView` from the repository root:

```powershell
mvn -pl notification-service -am -DskipTests package
Set-Location .\notification-service
java -jar target\notification-service-1.0.0.jar --retail.projection.replay.enabled=true --retail.events.streams.enabled=false --retail.events.consumer.enabled=false --retail.events.publishing.enabled=false --retail.events.outbox.enabled=false --spring.main.web-application-type=none
```

Kafka is the live transport, not the replay archive. Each service's database event log is the replay source, so replay does not republish historical events or trigger downstream side effects. Notification Service remains CRUD-based and its events describe notification creation; aggregate reconstruction, snapshots, and full event sourcing are not implemented.

\---
