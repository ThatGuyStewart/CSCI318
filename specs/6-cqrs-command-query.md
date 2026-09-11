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

## 3\. Application Service Split

### 3.1 Command Application Service

* Accepts command requests from API endpoints.
* Routes to command handlers.
* Loads and executes aggregate behavior.
* Persists changes (event append or CRUD write).
* Publishes domain events for read model updates and integrations.

### 3.2 Query Application Service

* Accepts read requests from API endpoints.
* Routes to query handlers.
* Reads from projection/read stores only.
* Shapes response DTOs for API contracts.

### Constraint

* Queries must not call domain mutation methods.

\---

## 4\. Write and Read Models

### 4.1 Write Model (Aggregates)

* Event-sourced aggregates: `Customer`, `Product`, `Order`.
* CRUD aggregate: `Notification`.

### 4.2 Read Model (Projections)

* `CustomerView`: driven by `CustomerCreatedEvent`, `CustomerUpdatedEvent`, `CustomerDeletedEvent`, `BasketItemAddedEvent`, `BasketItemRemovedEvent`, `BasketRecalculatedEvent`. Used by customer queries (C1, C2, C3).
* `ProductView`: driven by `ProductCreatedEvent`, `ProductUpdatedEvent`, `ProductDeletedEvent`. Used by product queries (P2).
* `OrderSummaryView`: driven by `OrderPlacedEvent`, `OrderStatusChangedEvent`, `OrderCancelledEvent`. Used by order queries (O1, O2).
* **Notification Service** persists notifications in a CRUD store. No projection; reads are from the CRUD entity store.

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
|O2|Get/update Orders (admin queries)|Query|No|
|O3|Get Order events|Query|No|
|N1|Create/get Notifications|Command + Query|No|

\---

## 6\. Command Catalog

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
|`CreateNotificationCommand`|`Notification`|N1|`NotificationCreatedEvent`|CRUD store + Event publish|
|`CreateNotificationBroadcastCommand`|`Notification`|N1|`NotificationCreatedEvent` (multiple)|CRUD store + Event publish|
|`CreateNotificationAreaBroadcastCommand`|`Notification`|N1|`NotificationCreatedEvent` (filtered by area)|CRUD store + Event publish|

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
|`GetCustomerBasketQuery(customerId)`|C3|`CustomerView`|Eventual||
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
|`ListOrderEventsByCustomerQuery(customerId, dateRange?)`|O3|Event store|Strong|Returns persisted `DomainEventEnvelope` records|
|`GetNotificationByIdQuery(notificationId)`|N1|Notification CRUD store|Strong||
|`ListNotificationsByCustomerQuery(customerId, dateRange?)`|N1|Notification CRUD store|Strong||
|`ListNotificationsByCustomerEmailQuery(email, dateRange?)`|N1|Notification CRUD store|Strong||
|`ListNotificationsByCustomerPhoneQuery(phone, dateRange?)`|N1|Notification CRUD store|Strong||
|`ListAllNotificationsQuery(dateRange?)`|N1|Notification CRUD store|Strong||
|`ListNotificationsByDateQuery(date)`|N1|Notification CRUD store|Strong||



