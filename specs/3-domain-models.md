# Domain Model Specification



This document describes the current **write models**, **read models**, **aggregates**, **domain events**, **invariants**, and **projection rules** for the Online Retail Microservice System. Event records are persisted alongside mutable entities; they are not currently the authoritative aggregate state.

It reflects the current architecture: **CQRS-style read models**, **domain events**, and **AI-driven product recommendations**.



\---



# 1\. Aggregate Overview



## 1.1 Customer Aggregate

**Aggregate Root:** `Customer`

**Entities / Value Objects:** `Address`, `Basket`, `BasketItem`

**Domain Events:**

* `CustomerCreatedEvent`
* `CustomerUpdatedEvent`
* `CustomerDeletedEvent`
* `BasketItemAddedEvent`
* `BasketItemRemovedEvent`
* `BasketRecalculatedEvent`
* `BasketClearedEvent`
* `BasketClearedAfterOrderEvent`



## 1.2 Product Aggregate

**Aggregate Root:** `Product`

**Domain Events:**

* `ProductCreatedEvent`
* `ProductUpdatedEvent`
* `ProductDeletedEvent`



## 1.3 Order Aggregate

**Aggregate Root:** `Order`

**Entities / Value Objects:** `Address`, `OrderItem`

**Domain Events:**

* `OrderPlacedEvent`
* `BasketUsedForOrderEvent`
* `OrderStatusChangedEvent`
* `OrderCancelledEvent`
* `OrderCancelFailedEvent`



## 1.4 Notification Aggregate

**Aggregate Root:** `Notification`



\---



# 2\. Write Models (Command Side)



## 2.1 Customer Service Domain Model

The Customer Service manages the record of customers, their addresses, and their shopping baskets. A customer has 1 address and 1 basket.



## 2.1.1 `Customer` (Entity, Aggregate Root)

Represents a customer.

|Field Name|Type|Description|Constraints / Notes|
|-|-|-|-|
|`customerId`|`Long`|Unique identifier for the customer|Primary Identifier|
|`name`|`String`|Name of the customer|Required|
|`email`|`String`|Email address of the customer|Required. Unique|
|`phone`|`String`|Phone number of the customer|Required nonblank string. Unique; no numeric or phone-pattern validation|
|`contactMethod`|`Enum`|Customer's preferred contact method. ContactMethod (`Email`, `Phone`)|`Email` by default|
|`address`|`Address`|Customer's residential address|Required|



### Invariants

* Email and phone must be unique.
* Name, email and phone must be nonblank on creation. Email syntax and numeric phone positivity are not validated.
* Address must satisfy its own invariants.



**Domain Events (examples)**

* `CustomerCreatedEvent` – emitted when a new customer is created
* `CustomerUpdatedEvent` – emitted when customer details change





\---



### 2.1.2 `Address` (Value Object)

Represents a customer's residential address. It is a separate embeddable domain class.

|Field Name|Type|Description|Constraints / Notes|
|-|-|-|-|
|`unitNumber`|`Int`|The number of the unit/flat|Optional. Positive value > 0|
|`streetNumber`|`Int`|The street number of the building|Required. Positive value > 0|
|`street`|`String`|The street name|Required|
|`suburb`|`String`|The suburb name|Optional|
|`city`|`String`|The city name|Required|
|`postcode`|`Int`|The postcode for the address|Required. Positive value > 0|
|`state`|`String`|The state initials|Required|
|`country`|`String`|The country name|Required|

\---



## 2.1.3 `Basket` (Entity)

Represents a collection of products a customer is interested in purchasing. Automatically created when a new customer record is created. Only 1 basket per customer may exist.

|Field Name|Type|Description|Constraints / Notes|
|-|-|-|-|
|`customerId`|`Long`|The customer id of the customer whose basket this is|Primary Identifier (same as customer id. A `customer` has only 1 `basket`)|
|`items`|`List<BasketItem>`|The products within the basket|Each item carries a `productId`. There should be at most one item per product; adding the same product increments its quantity. Items with quantity below 1 are removed.|
|`total`|`Double`|The total cost of the items in the basket|Cannot be set manually. The value is derived from the sum of the item `subtotal` values and recalculated when items change.|



### Domain Rules

* Total is derived from item subtotals.
* Add/remove request quantities must be positive; invalid quantities return 400. A removal whose resulting quantity is below 1 removes the item entirely.
* Updating a product's name or price triggers basket item updates via `ProductUpdatedEvent`.
* Placing an order containing basket contents triggers the removal all products from basket, via `BasketUsedForOrderEvent`.
* Basket total recalculates automatically.



**Domain Events (examples)**

* `BasketItemAddedEvent` – emitted when an item is added
* `BasketItemRemovedEvent` – emitted when an item is removed
* `BasketRecalculatedEvent` – emitted when an external product update changes basket items values (may require item subtotal and basket total to be recalculated); customer item additions and removals are represented by their respective item events
* `BasketClearedEvent` – emitted when a non-empty basket is explicitly cleared
* `BasketClearedAfterOrderEvent` – emitted by Customer Service after consuming `BasketUsedForOrderEvent`; its causation ID references the Order event



\---



### 2.1.4 `BasketItem` (Embeddable Value Object)

Represents a product within a customer's basket. This class should be updated if its corresponding product is updated.



|Field Name|Type|Description|Constraints / Notes|
|-|-|-|-|
|`productId`|`Long`|Product identifier|List element field, not a map key|
|`name`|`String`|The product name|Resolved through Product Service; ProductUpdatedEvent refreshes it asynchronously|
|`price`|`Double`|The price of a single unit|Resolved through Product Service; ProductUpdatedEvent refreshes it asynchronously|
|`quantity`|`Int`|The number of units of this product within the basket|required. Positive value > 0. If a basket already contains this product, adding or removing a quantity increments or decrements it. If the resulting quantity is less than 1, the corresponding list item is removed from the basket.|
|`subtotal`|`Double`|The total price for the product|Derived from `price` * `quantity`|

Implementation limitation: a missing Product lookup (404/empty) uses the name `Product <id>` and price 10.0 instead of rejecting the basket item. A downstream connectivity/error response is handled as a service-unavailable error.





## 2.2 Product Service Domain Model

The Product Service manages products.



## 2.2.1 `Product` (Entity, Aggregate Root)

Represents a product.

|Field Name|Type|Description|Constraints / Notes|
|-|-|-|-|
|`productId`|`Long`|Unique product identifier|Primary Identifier|
|`name`|`String`|Name of the product|Required. Unique. ProductUpdatedEvent asynchronously updates matching BasketItem.productId entries|
|`category`|`Enum`|The category of products the product is in. Enum (`Electronics`, `Appliances`, `Furniture`, `Kitchen`, `Tools`, `Garden`, `Sports`, `Toys`, `Automotive`, `Pets`, `Apparel`, `Beauty`, `Grocery`, `Media`, `Professional`, `Lifestyle`)|Required|
|`price`|`Double`|The price for a single unit of the product|Required and positive. Changes update matching basket items asynchronously; existing order snapshots are unchanged|
|`description`|`String`|A brief description of the product|Optional|



### Domain Rules

* Updating name/price emits `ProductUpdatedEvent`.
* Customer Service consumes this event to update basket items.



**Domain Events (examples)**

* `ProductCreatedEvent` - emitted when a product is created.
* `ProductUpdatedEvent` - emitted when a product is updated.
* `ProductDeletedEvent` - emitted when a product is deleted.



\---



## 2.3 Order Service Domain Model

The Order Service manages orders for products, and the delivery of orders.



## 2.3.1 `Order` (Entity, Aggregate Root)

Represents an order for a selection of products, and tracks the status of the order.

|Field Name|Type|Description|Constraints / Notes|
|-|-|-|-|
|`orderId`|`Long`|Unique order record identifier|Primary Identifier|
|`customerId`|`Long`|Reference to the customer who placed the order|Required; captured at creation|
|`address`|`Address`|Delivery-address snapshot|Required after resolution; omitted request address is copied from the customer|
|`items`|`List<OrderItem>`|The products ordered|Required. Each item carries a `productId`, a product snapshot, and a quantity.|
|`total`|`Double`|The total cost of the ordered products|Derived from the sum of OrderItem subtotals; snapshot semantics, not Java final fields|
|`status`|`Enum`|The status of the order. Enum (`Placed`, `Pending`, `InTransit`, `Delivered`, `Cancelled`)|`Placed` by default|



### Domain Rules



– Order Creation –



* If the customer provides a product ID and quantity: The order contains the given quantity of the product matching the ID
* If items are omitted/null, the order uses basket contents and publishes BasketUsedForOrderEvent to clear the basket asynchronously. An explicit empty items list is invalid; explicit item-list checkout does not clear the basket.
* If the customer provides an address: The address for the order is the address provided
* If the customer provides no address: The address for the order is the address located in the customer's record.
* A new order has status "Placed". Kafka-driven notification recording is asynchronous, not a direct call or external delivery.



**Domain Events (examples)**

* `OrderPlacedEvent` - emitted when an order is created.
* `OrderStatusChangedEvent` - emitted when an order's status changes.
* `BasketUsedForOrderEvent` - emitted, in addition to `OrderPlacedEvent`, when an order is created using a customer basket.



– Order Cancellation –



* An order can only be cancelled if its status is "Placed" or "Pending". If a customer tries to cancel an order whose status is not "Placed" or "Pending", the order is unchanged and a notification is automatically generated, notifying the customer that the order could not be cancelled, and suggesting the client call customer service if they require assistance
* If an order is successfully cancelled, its status becomes "Cancelled" and a notification is automatically generated, notifying the customer that their order has been cancelled



**Domain Events (examples)**

* `OrderCancelledEvent` - emitted when an order is cancelled.
* `OrderCancelFailedEvent` - emitted when an order cancellation fails.



\---



### 2.3.2 `Address` (Value Object)

Represents the delivery-address snapshot for an order (same fields as section 2.1.2). Omitting the address in a create request copies the customer's address; a resolved address is required. Snapshot semantics do not mean the Java fields are declared `final`.

\---



### 2.3.3 `OrderItem` (Embeddable Value Object)

Represents a product within an order. Item values are snapshots captured when the order is placed.



|Field Name|Type|Description|Constraints / Notes|
|-|-|-|-|
|`productId`|`Long`|Product identifier|List element field|
|`name`|`String`|Product name snapshot|Resolved from Product for explicit items, or from Basket for basket checkout|
|`price`|`Double`|Unit price snapshot|Resolved server-side; not updated by later Product events|
|`quantity`|`Int`|Number of units ordered|Required, positive|
|`subtotal`|`Double`|Total price for the item|Derived from `price` * `quantity`|

\---



## 2.4 Notification Service Domain Model

The Notification Service manages notifications to customers.



## 2.4.1 `Notification` (Entity, Aggregate Root)

Represents a notification to a customer.

**Domain Events:**

* `NotificationCreatedEvent`

|Field Name|Type|Description|Constraints / Notes|
|-|-|-|-|
|`notificationId`|`Long`|Unique notification record identifier|Primary Identifier|
|`customerId`|`Long`|Customer receiving the notification|Required|
|`type`|`Enum`|Selected delivery type (Email, Phone)|Uses the customer's contactMethod; no external delivery provider is implemented|
|`message`|`String`|The message being delivered to the customer|Required|
|`sent`|`LocalDateTime`|Notification record creation time|Exposed as `sentAt` in response/event DTOs; not proof of external delivery|

\---





# 3\. Read Models (Query Side)



Read models are persisted query views.

CustomerView, ProductView, OrderSummaryView, and NotificationView are persisted read models. Each is updated from its service-owned Kafka Streams topology and can be rebuilt from that service's persisted event log. Notification Service remains CRUD-based; its event log records notification creation, not aggregate state transitions.



## 3.1 CustomerView

|Field|Type|
|-|-|
|customerId|Long|
|name|String|
|email|String|
|phone|String|
|contactMethod|Enum (Phone, Email)|
|address|Address|
|basketTotal|Double|

Owner: Customer aggregate



Updated by:

* `CustomerCreatedEvent`
* `CustomerUpdatedEvent`
* `CustomerDeletedEvent`
* `BasketItemAddedEvent`
* `BasketItemRemovedEvent`
* `BasketRecalculatedEvent`
* `BasketClearedEvent`
* `BasketClearedAfterOrderEvent`



\---



## 3.2 ProductView

|Field|Type|
|-|-|
|productId|Long|
|name|String|
|category|Enum|
|price|Double|
|description|String|
|aggregateVersion|Long|



Owner: Product aggregate



Updated by:

* `ProductCreatedEvent`
* `ProductUpdatedEvent`
* `ProductDeletedEvent`



\---



## 3.3 OrderSummaryView

|Field|Type|
|-|-|
|orderId|Long|
|customerId|Long|
|items|List<OrderItem>|
|total|Double|
|address|Address|
|status|Enum|
|createdAt|LocalDateTime|
|productIds|List<Long>|



Owner: Order aggregate



Updated by:

* `OrderPlacedEvent`
* `BasketUsedForOrderEvent`
* `OrderStatusChangedEvent`
* `OrderCancelledEvent`
* `OrderCancelFailedEvent`



\---





## 3.4 NotificationView

|Field|Type|
|-|-|
|notificationId|Long|
|customerId|Long|
|type|DeliveryType (Email, Phone)|
|message|String|
|sent|LocalDateTime|

Updated by `NotificationCreatedEvent`. Its `sent` field maps to `sentAt` in the API response.

All four services also persist aggregate-version checkpoints for projection idempotency. ProductView additionally stores aggregateVersion on the view itself.

# 4\. Domain Events



## Persisted Event Records

Customer, Product, Order and Notification services persist append-only domain-event records in their service databases.
Events are not updated or deleted by application operations.
CustomerView, ProductView, OrderSummaryView, and NotificationView can be rebuilt from their owning service's persisted events. CustomerView replay also reconciles its basket total from the current mutable Basket state for compatibility with historical checkouts that did not write a Customer basket-clear event. Notification remains CRUD-based; its events describe notification creation. No aggregate snapshot or full event-sourcing mechanism is implemented.





# 5\. Event Consumption Rules

All four services consume their own event topic through Kafka Streams to update their projection. The following rules describe additional cross-service listeners.


## Customer Service

Consumes `product.events`:

* `ProductUpdatedEvent` → updates basket items if the product's name or price is updated and the product is in the basket.
* `ProductDeletedEvent` → updates basket items if the deleted product is in the basket.

Consumes `order.events`:

* `BasketUsedForOrderEvent` → updates basket items.
* Consumer records processed `eventId` values to prevent duplicate application.



## Product Service

Consumes:

* No additional cross-service topics (its own product.events projection is still consumed).



## Order Service

Order Service currently uses its REST clients to obtain customer information when processing commands. It does not consume `customer.events`.



## Notification Service

Consumes `order.events`:

* `OrderPlacedEvent`. → Creates a notification to the customer {customerId} that an order {orderId} has been placed
* `OrderCancelledEvent`. → Creates a notification to the customer {customerId} that an order {orderId} was cancelled
* `OrderCancelFailedEvent`. → Creates a notification to the customer {customerId} that an order {orderId} could not be cancelled, and to contact customer support.
* Consumer records processed `eventId` values to prevent duplicate application.

\---





# 6\. DTO Mapping Rules (AI / LangChain4j)



Natural language -> RecommendationAgent -> read-only Tools -> product IDs -> catalogue-validated ProductResponse list

The implemented agent recommends products only; it cannot add basket items, place orders or issue other commands. See [7-AI-agent.md](7-AI-agent.md).



\---



\*\*End of file\*\*
