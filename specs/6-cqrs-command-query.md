# CQRS Command and Query Specification

## 1\. Purpose

This document defines how Services apply CQRS, including:

1. Separation between command and query application services.
2. Write/read model responsibilities.
3. User story mapping to command or query.
4. Command and query catalogs.
5. Handler wiring and read model update flow.
6. Consistency model and error model.

\---

## 2\. CQRS Principles

1. **Commands mutate state** and execute domain invariants through aggregates.
2. **Queries read state** from read models/projections and do not mutate domain state.
3. Write and read models may use different schemas optimized for their workloads.
4. Read side may be eventually consistent with the write side.

\---

## 3\. Logical Application Service Responsibilities

Command/query names in this document are a conceptual catalogue, not implemented class names. Controllers currently share `@Service` implementations; there is no separate command/query bus or handler-class hierarchy.

### 3.1 Command Application Service

* Accepts command requests from API endpoints.
* Calls the corresponding application-service operation.
* Loads and executes aggregate behavior.
* Persists changes (event append or CRUD write).
* Publishes domain events for read model updates and integrations.

### 3.2 Query Application Service

* Accepts read requests from API endpoints.
* Calls the corresponding application-service operation.
* Reads projections for most resource queries, persisted records for event history, and mutable Basket/BasketItem data for basket queries.
* Shapes response DTOs for API contracts.

### Constraint

* Queries should not mutate business aggregates. Current exceptions: basket GET can create a missing basket, and recommendations update in-memory conversation state.

\---

## 4\. Write and Read Models

### 4.1 Write Model (Aggregates)

* Mutable command-side entities with persisted domain-event records: `Customer`, `Product`, `Order`.
* CRUD aggregate with persisted creation events: `Notification`.

### 4.2 Read Model (Projections)

* `CustomerView`: driven by Customer profile and basket events through Customer Service's Kafka Streams topology. It can be rebuilt from the Customer event log; replay reconciles basket total from the mutable Basket table to accommodate historical order-driven clears.
* `ProductView`: driven by `ProductCreatedEvent`, `ProductUpdatedEvent`, `ProductDeletedEvent` through Product Service's Kafka Streams topology. The projection can be rebuilt from its persisted event log.
* `OrderSummaryView`: driven by order snapshot events through Order Service's Kafka Streams topology and rebuilt from its persisted event log.
* `NotificationView`: driven by `NotificationCreatedEvent` through Notification Service's Kafka Streams topology and rebuilt from its persisted event log. Notification writes remain CRUD-based.

\---

## 5\. User Story Classification (C1-C4, P1-P3, O1-O3, N1)

|User Story ID|User Story|CQRS Type|Through Aggregate|
|-|-|-|-|
|C1|Create/get/update/delete a Customer|Command + Query|Yes|
|C2|Get Customers (admin queries)|Query|No|
|C3|Get/update Basket|Command + Query|Yes|
|C4|Get Customer events|Query|No|
|P1|Create/update/delete Products|Command|Yes|
|P2|Get/search Products|Query|No|
|P3|Get Product events|Query|No|
|O1|Create/get/update/cancel Orders|Command + Query|Yes|
|O2|Get/update Orders (admin queries)|Command + Query|Yes|
|O3|Get Order events|Query|No|
|N1|Create/get Notifications|Command + Query|No|

\---

## 6\. Command Catalog

The persistence column below denotes the event log, not the sole write store. Commands also persist mutable entities and, when enabled, an outbox record in the same database transaction. Notification creation likewise persists its event; it is not publish-only. No-op basket operations and broadcasts to no recipients need not append an event.

|Command|Target Aggregate(s)|Story|Output Events|Persistence|
|-|-|-|-|-|
|`CreateCustomerCommand`|`Customer`|C1|`CustomerCreatedEvent`|Event store|
|`UpdateCustomerCommand`|`Customer`|C1|`CustomerUpdatedEvent`|Event store|
|`UpdateCustomerAddressCommand`|`Customer`|C1|`CustomerUpdatedEvent`|Event store|
|`DeleteCustomerCommand`|`Customer`|C1|`CustomerDeletedEvent`|Event store|
|`AddBasketItemCommand`|`Customer`|C3|`BasketItemAddedEvent`|Event store|
|`RemoveBasketItemCommand`|`Customer`|C3|`BasketItemRemovedEvent`|Event store|
|`CreateProductCommand`|`Product`|P1|`ProductCreatedEvent`|Event store|
|`UpdateProductCommand`|`Product`|P1|`ProductUpdatedEvent`|Event store|
|`DeleteProductCommand`|`Product`|P1|`ProductDeletedEvent`|Event store|
|`CreateOrderCommand`|`Order`|O1|`OrderPlacedEvent`, `BasketUsedForOrderEvent` (if from basket)|Event store|
|`CancelOrderCommand`|`Order`|O1|`OrderCancelledEvent` (if cancelled successfully), `OrderCancelFailedEvent` (if order could not be cancelled)|Event store|
|`UpdateOrderStatusCommand`|`Order`|O2|`OrderStatusChangedEvent`|Event store|
|`CreateNotificationCommand`|`Notification`|N1|`NotificationCreatedEvent`|Entity + event log + outbox|
|`CreateNotificationBroadcastCommand`|`Notification`|N1|`NotificationCreatedEvent` (multiple)|Entity + event log + outbox|
|`CreateNotificationAreaBroadcastCommand`|`Notification`|N1|`NotificationCreatedEvent` (filtered by area)|Entity + event log + outbox|

Command responses confirm validation and event persistence. The API response may include the created or updated resource as a DTO; subsequent GET requests read projections and can temporarily lag the command.

\---

## 7\. Query Catalog

|Query|Story|Read Source|Consistency|Notes|
|-|-|-|-|-|
|`GetCustomerByIdQuery(customerId)`|C1, C2|`CustomerView`|Eventual||
|`GetCustomerByEmailQuery(email)`|C1, C2|`CustomerView`|Eventual||
|`GetCustomerByPhoneQuery(phone)`|C1, C2|`CustomerView`|Eventual||
|`ListAllCustomersQuery()`|C2|`CustomerView`|Eventual||
|`ListCustomersByStateQuery(state)`|C2|`CustomerView`|Eventual||
|`ListCustomersByCountryQuery(country)`|C2|`CustomerView`|Eventual||
|`ListCustomersByPostcodeQuery(postcode)`|C2|`CustomerView`|Eventual||
|`GetCustomerBasketQuery(customerId)`|C3|Basket/BasketItem, with CustomerView existence check|Mixed|Basket contents are command-side data; existence and cross-service updates can lag|
|`GetCustomerEventsQuery(customerId, dateRange?)`|C4|Event store|Strong|Returns persisted `DomainEventEnvelope` records|
|`ListAllCustomerEventsQuery(dateRange?)`|C4|Event store|Strong|Returns persisted `DomainEventEnvelope` records|
|`GetProductByIdQuery(productId)`|P2|`ProductView`|Eventual||
|`ListAllProductsQuery()`|P2|`ProductView`|Eventual||
|`ListProductsByCategoryQuery(category)`|P2|`ProductView`|Eventual||
|`SearchProductsByNameQuery(name)`|P2|`ProductView`|Eventual|Partial match|
|`GetProductEventsQuery(productId, dateRange?)`|P3|Event store|Strong|Returns persisted `DomainEventEnvelope` records|
|`ListAllProductEventsQuery(dateRange?)`|P3|Event store|Strong|Returns persisted `DomainEventEnvelope` records|
|`ListProductEventsByCategoryQuery(category, dateRange?)`|P3|Event store|Strong|Returns persisted `DomainEventEnvelope` records|
|`GetOrderByIdQuery(orderId)`|O1, O2|`OrderSummaryView`|Eventual||
|`ListOrdersByCustomerQuery(customerId)`|O1, O2|`OrderSummaryView`|Eventual||
|`ListOrdersByCustomerEmailQuery(email)`|O2|`OrderSummaryView`|Eventual||
|`ListOrdersByCustomerPhoneQuery(phone)`|O2|`OrderSummaryView`|Eventual||
|`ListOrdersByProductQuery(productId)`|O2|`OrderSummaryView`|Eventual||
|`GetOrderEventsQuery(orderId, dateRange?)`|O3|Event store|Strong|Returns persisted `DomainEventEnvelope` records|
|`ListAllOrderEventsQuery(dateRange?)`|O3|Event store|Strong|Returns persisted `DomainEventEnvelope` records|
|`ListOrderEventsByCustomerQuery(customerId, dateRange?)`|O3|OrderSummaryView IDs then event store|Mixed|Event records are direct; discovery of orders is eventual|
|`GetNotificationByIdQuery(notificationId)`|N1|`NotificationView`|Eventual||
|`ListNotificationsByCustomerQuery(customerId, dateRange?)`|N1|`NotificationView`|Eventual||
|`ListNotificationsByCustomerEmailQuery(email, dateRange?)`|N1|`NotificationView`|Eventual||
|`ListNotificationsByCustomerPhoneQuery(phone, dateRange?)`|N1|`NotificationView`|Eventual||
|`ListAllNotificationsQuery(dateRange?)`|N1|`NotificationView`|Eventual||
|`ListNotificationsByDateQuery(date)`|N1|`NotificationView`|Eventual||
|`ListNotificationsByDateRangeQuery(from, to)`|N1|`NotificationView`|Eventual||
|`RecommendProductsQuery(message, sessionId?)`|AI|ProductView, embedding store, optional Customer/Order REST tools|Mixed|Read-only business tools; model output is not authoritative data|

## 8. Wiring, Consistency and Errors

With streams enabled, command transactions persist write entities, events and outbox records. The dispatcher publishes asynchronously and the owning Kafka Streams topology applies projections. With streams disabled, the command path applies its own projection synchronously. Cross-service listeners are independently controlled by `retail.events.consumer.enabled`.

At-least-once delivery is handled by aggregate-version projection checkpoints and processed-event IDs for cross-service listeners. Kafka ordering is per partition/key; outbox retries and concurrent publishers do not establish an unconditional aggregate-version publication-order guarantee.

Resource GETs can briefly return stale data or 404 after successful commands. Basket clearing and notification creation after checkout are asynchronous. Database-event replay is an offline maintenance operation, not full aggregate reconstruction; see [5-event-sourcing.md](5-event-sourcing.md).

Handled request validation, missing resources and uniqueness conflicts use 400, 404 and 409 respectively. Downstream REST/service failures mapped by the application and unavailable recommendations use 503; other unhandled exceptions use 500. Ineligible cancellation is a business outcome returning 200 with unchanged status and `OrderCancelFailedEvent`, not a transport error. Status updates validate the enum but do not enforce a forward-only workflow.
