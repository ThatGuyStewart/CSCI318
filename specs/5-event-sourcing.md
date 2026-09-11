# Event Sourcing Specification

## 

## 1\. Scope

* This specification defines event sourcing for **Customer Service**, **Product Service**, **Order Service**.
* **Notification Service** remains CRUD-based.

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
|`aggregateType`|String|Aggregate which created the event (`Customer`, `Product`, `Order`)|
|`eventType`|String|One of the event types listed in section 3.3|
|`aggregateVersion`|Long|Monotonically increasing version of the aggregate stream after this event|
|`occurredAt`|timestamp|Wall-clock time the event was produced|
|`correlationId`|UUID|Links a request/workflow|
|`causationId`|UUID|Event or command that caused this event|
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
|`Product`, `Product`|`ProductCreatedEvent`|Creation of `Product` record|`productId`, `name`, `category`, `price`, `description`|
|`Product`, `Product`|`ProductUpdatedEvent`|Update a `Product` record|`productId`, Any updated fields (`name`, `category`, `price`, `description`)|
|`Product`, `Product`|`ProductDeletedEvent`|Delete a `Product` record|`productId`|
|`Order`|`OrderPlacedEvent`|Creation of `Order` record|`orderId`, `customerId`, `address`, `items`, `total`, `status`|
|`Order`|`BasketUsedForOrderEvent`|A created `Order` uses a `Customer` `Basket` is used for `items` contents|`customerId`|
|`Order`|`OrderStatusChangedEvent`|The `status` of an `Order` is updated|`orderId`, `status`|
|`Order`|`OrderCancelledEvent`|An `Order` is cancelled|`orderId`, `customerId`|
|`Order`|`OrderCancelFailedEvent`|An `Order` cancellation cannot be completed|`orderId`, `customerId`|

\---

