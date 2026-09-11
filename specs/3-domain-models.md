# Domain Model Specification



This document defines the **write models**, **read models**, **aggregates**, **domain events**, **invariants**, and **projection rules** for the Online Retail Microservice System.

It reflects the updated architecture: **CQRS**, **event sourcing**, **aggregates**, **domain events**, and **AI‑driven DTO mapping**.



\---



# 1\. Aggregate Overview



## 1.1 Customer Aggregate

**Aggregate Root:** `Customer`

**Entities / Value Objects:** `Address`, `Basket`, `Item`

**Domain Events:**

* `CustomerCreatedEvent`
* `CustomerUpdatedEvent`
* `CustomerDeletedEvent`
* `BasketItemAddedEvent`
* `BasketItemRemovedEvent`
* `BasketRecalculatedEvent`



## 1.2 Product Aggregate

**Aggregate Root:** `Product`

**Domain Events:**

* `ProductCreatedEvent`
* `ProductUpdatedEvent`
* `ProductDeletedEvent`



## 1.3 Order Aggregate

**Aggregate Root:** `Order`

**Entities / Value Objects:** `Address`, `Item`

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
|`phone`|`String`|Phone number of the customer|Required. Unique, not negative|
|`contactMethod`|`Enum`|Customer's preferred contact method. ContactMethod (`Email`, `Phone`)|`Email` by default|
|`address`|`Address`|Customer's residential address|Required|



### Invariants

* Email and phone must be unique.
* Phone must be positive.
* Address must satisfy its own invariants.



**Domain Events (examples)**

* `CustomerCreatedEvent` – emitted when a new customer is created
* `CustomerUpdatedEvent` – emitted when customer details change





\---



### 2.1.2 `Address` (Value Object)

Represents a customer's residential address. It is a nested class within the Customer class.

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
|`items`|`HashMap<Long, Item>`|A list of products within the basket|The key is a `productId`, the value is an `Item` class. If the quantity of an `Item` in the `HashMap` becomes < 1, its key and value will be removed from the `HashMap`|
|`total`|`Double`|The total cost of the items in the basket|Cannot be set manually. The value is derived from the sum of the `Item` `subtotal` values in `items`. This value should be updated automatically if any `subtotal` changes in an `Item` class, or if a `HashMap` entry is deleted within the `items` `HashMap`.|



### Domain Rules

* Total is derived from item subtotals.
* Removing quantity < 1 removes the item entirely.
* Updating a product's name or price triggers basket item updates via `ProductUpdatedEvent`.
* Placing an order containing basket contents triggers the removal all products from basket, via `BasketUsedForOrderEvent`.
* Basket total recalculates automatically.



**Domain Events (examples)**

* `BasketItemAddedEvent` – emitted when an item is added
* `BasketItemRemovedEvent` – emitted when an item is removed
* `BasketRecalculatedEvent` – emitted when an external product update changes basket items values (may require item subtotal and basket total to be recalculated); customer item additions and removals are represented by their respective item events



\---



### 2.1.4 `Item` (Value Object)

Represents a product within the Items HashMap of a customer's basket. This class should be updated if its corresponding product is updated.



|Field Name|Type|Description|Constraints / Notes|
|-|-|-|-|
|`name`|`String`|The product name|Cannot be set manually. Value derived from `Product` service by the `product` `id` given as the `items` key in a `customer` `basket`. This value should be updated automatically if the `product` `name` is updated.|
|`price`|`Double`|The price of a single unit of this product|Cannot be set manually. Value derived from `Product` service by the `productId` given as the `items` key in a `customer` `basket`. This value should be automatically updated if the `product` `price` is updated|
|`quantity`|`Int`|The number of units of this product within the basket|required. Positive value > 0. If a `basket` already contains this `product` and a number of this `product` is added to the `basket`, the `quantity` should be incremented by that amount, and similarly if a number of this `product` is removed from the `basket`, the `quantity` should be decremented by that amount. If the resulting `quantity` would become < 1, the corresponding `HashMap` entry will be automatically removed from the `basket`|
|`subtotal`|`Double`|The total price for the product|Not negative. Cannot be set manually. Value is derived from `unitPrice` \* `quantity`. It should be updated automatically if either `unitPrice` or `quantity` changes.|





## 2.2 Product Service Domain Model

The Product Service manages products.



## 2.2.1 `Product` (Entity, Aggregate Root)

Represents a product.

|Field Name|Type|Description|Constraints / Notes|
|-|-|-|-|
|`productId`|`Long`|Unique product identifier|Primary Identifier|
|`name`|`String`|Name of the product|Required. If changed, automatically updates all `name` fields in `Item` classes in `Customer` `Basket` `items` whose key matches `productId`|
|`category`|`Enum`|The category of products the product is in. Enum (`Electronics`, `Appliances`, `Furniture`, `Kitchen`, `Tools`, `Garden`, `Sports`, `Toys`, `Automotive`, `Pets`, `Apparel`, `Beauty`, `Grocery`, `Media`, `Professional`, `Lifestyle`)|Required|
|`price`|`Double`|The price for a single unit of the product|Required. If changed, automatically updates all `price` fields in `Item` classes in `Customer` `Basket` `items` whose key matches `productId`|
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
|`customerId`|`Long`|Reference to the customer who placed the order|Required. Final.|
|`address`|`Address`|The delivery address for the order|Optional. If an `address` is not supplied, the `address` of the `customer` with matching `customerId` will be automatically copied as the `address` for the `Order`. Final|
|`items`|`HashMap<Long, Item>`|A list of products ordered|Required. Final. The key is a `productId`, the value is an `Item` class.|
|`total`|`Double`|The total cost of the ordered products|Cannot be set manually. The value is derived from the sum of the `Item` `subtotal` values in `items`. Final|
|`status`|`Enum`|The status of the order. Enum (`Placed`, `Pending`, `InTransit`, `Delivered`, `Cancelled`)|`Placed` by default|



### Domain Rules



– Order Creation –



* If the customer provides a product ID and quantity: The order contains the given quantity of the product matching the ID
* If the customer provides no product ID: The order contains the products in the customer's basket and their basket becomes empty
* If the customer provides an address: The address for the order is the address provided
* If the customer provides no address: The address for the order is the address located in the customer's record.
* When an order is successfully created, its status is "Placed" and a notification is automatically generated, notifying the customer that the order has been placed



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

Represents a delivery address for an order. (Same as 2.2 `Address` but all fields are final).

\---



### 2.3.3 `Item` (Value Object)

Represents a product within the Items HashMap of a customer's basket. This class should be updated if its corresponding product is updated.



|Field Name|Type|Description|Constraints / Notes|
|-|-|-|-|
|`name`|`String`|The product name|Cannot be set manually. Value derived from `Product` service by the `product` `id` given as the `items` key in an `Order`. Final|
|`price`|`Double`|The price of a single unit of this product|Cannot be set manually. Value derived from `Product` service by the `product` `id` given as the `items` key in an `Order`. Final|
|`quantity`|`Int`|The number of units of the product ordered|required. Positive value > 0. Final|
|`subtotal`|`Double`|The total price for the product|Not negative. Cannot be set manually. Value is derived from `unitPrice` \* `quantity`. Final|

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
|`type`|`Enum`|Method used to deliver the message.|Automatically uses the `contactMethod` of the `customer`|
|`message`|`String`|The message being delivered to the customer|Required|
|`sentAt`|`LocalDateTime`|Date and time when notification was sent|Set on creation|

\---





# 3\. Read Models (Query Side)



Read models are projections updated by domain events.

CustomerView, ProductView, and OrderSummaryView are separate persisted read models updated by projectors.



## 3.1 CustomerView

|Field|Type|
|-|-|
|customerId|Long|
|name|String|
|email|String|
|phone|String|
|contactMethod|Enum (Phone, Email)|
|address|Address|
|basket|Basket|

Owner: Customer aggregate



Updated by:

* `CustomerCreatedEvent`
* `CustomerUpdatedEvent`
* `CustomerDeletedEvent`
* `BasketItemAddedEvent`
* `BasketItemRemovedEvent`
* `BasketRecalculatedEvent`



\---



## 3.2 ProductView

|Field|Type|
|-|-|
|productId|Long|
|name|String|
|category|Enum|
|price|Double|



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
|items|HashMap<Long, Item>|
|total|Double|
|address|Address|
|status|Enum|
|createdAt|LocalDateTime|



Owner: Order aggregate



Updated by:

* `OrderPlacedEvent`
* `OrderStatusChangedEvent`
* `OrderCancelledEvent`



\---





# 4\. Domain Events



## Event Store

Customer, Product, and Order services own an append-only event store.
Events are never updated or deleted.
Aggregate state is rebuilt by replaying events, optionally from snapshots.





# 5\. Event Consumption Rules



## Customer Service

Consumes `product.events`:

* `ProductUpdatedEvent` → updates basket items if the product's name or price is updated and the product is in the basket.
* `ProductDeletedEvent` → updates basket items if the deleted product is in the basket.

Consumes `order.events`:

* `BasketUsedForOrderEvent` → updates basket items.
* Consumer records processed `eventId` values to prevent duplicate application.



## Product Service

Consumes:

* None (source of product events)



## Order Service

Consumes `customer.events`:

* `CustomerUpdatedEvent` (optional).
* Consumer records processed `eventId` values to prevent duplicate application.



## Notification Service

Consumes `order.events`:

* `OrderPlacedEvent`. → Creates a notification to the customer {customerId} that an order {orderId} has been placed
* `OrderCancelledEvent`. → Creates a notification to the customer {customerId} that an order {orderId} was cancelled
* `OrderCancelFailedEvent`. → Creates a notification to the customer {customerId} that an order {orderId} could not be cancelled, and to contact customer support.
* Consumer records processed `eventId` values to prevent duplicate application.

\---





# 6\. DTO Mapping Rules (AI / LangChain4j)



Natural language → Intent → DTO → Command → Event



Example:

* “Add 3 drills to my basket”

&#x20; → Intent: AddBasketItem

&#x20; → DTO: `BasketAddRequest(productId=..., quantity=3)`

&#x20; → Command: AddItemToBasket

&#x20; → Event: `BasketItemAddedEvent`



\---



\*\*End of file\*\*

