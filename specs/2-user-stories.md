# User Stories \& Specifications

## User Story Overview

|Service|User Story ID|User Story Name|Role(s)|
|-|-|-|-|
|**Customer Service**|C1|Create/get/update/delete a Customer|Customer|
|**Customer Service**|C2|Get Customers|Administrator|
|**Customer Service**|C3|Get/update Basket|Customer|
|**Customer Service**|C4|Get Customer events|Administrator|
|**Product Service**|P1|Create/update/delete Products|Administrator|
|**Product Service**|P2|Get/search Products|Customer, Administrator|
|**Product Service**|P3|Get Product events|Administrator|
|**Order Service**|O1|Create/get/update Orders|Customer|
|**Order Service**|O2|Get/update Orders|Administrator|
|**Order Service**|O3|Get Order events|Administrator|
|**Notification Service**|N1|Create/get Notifications|Administrator|
|**Product Service**|AI|Conversational product recommendations|Customer|

Role names describe intended actors; the prototype does not enforce authentication or authorisation.

## Customer Service



### User Story C1. Create/get/update/delete a Customer

* **As a** Customer, **I want to** create a customer record with details such as my name, email, phone (number), preferred contact method, and address
* **As a** Customer, **I want to** update my customer record if my details change
* **As a** Customer, **I want to** view my customer record
* **As a** Customer, **I want to** delete my customer record



### User Story C2: Get Customers

* **As an** Administrator, **I want to** retrieve a list of all customers
* **As an** administrator, **I want to** view a customer's record by supplying their ID
* **As an** administrator, **I want to** view a customer's record by supplying their email
* **As an** administrator, **I want to** view a customer's record by supplying their phone
* **As an** Administrator, **I want to** retrieve a list of all customers in a particular postcode
* **As an** Administrator, **I want to** retrieve a list of all customers in a particular state
* **As an** Administrator, **I want to** retrieve a list of all customers in a particular country





### User Story C3: Get/update Basket

* **As a** Customer, **I want to** view the details of the basket associated with my customer record, including the products in my basket and the total price of those products
* **As a** Customer, **I want to** add products to the basket associated with my customer record by supplying the product's ID and the quantity to add
* **As a** Customer, **I want to** remove a quantity of a product from the basket associated with my customer record



### User Story C4: Get Customer events

* **As an** Administrator, **I want to** retrieve a list of all events for a particular customer
* **As an** Administrator, **I want to** retrieve a list of all customer events between two dates
* **As an** Administrator, **I want to** retrieve a list of all events for a particular customer between two dates





## Product Service



### User Story P1: Create/update/delete Products

* **As an** administrator, **I want to** create a new product record with details such as name, category, and price
* **As an** administrator, **I want to** update a product's details
* **As an** administrator, **I want to** delete a product



### User Story P2: Get/search Products

* **As a** Customer / Administrator, **I want to** retrieve a list of all products
* **As a** Customer / Administrator, **I want to** retrieve a list of all products within a particular product category
* **As a** Customer / Administrator, **I want to** view a product's record by supplying its ID
* **As a** Customer / Administrator, **I want to** view all products whose names match a name I supply



### User Story P3: Get Product events

* **As an** Administrator, **I want to** retrieve a list of all product events
* **As an** Administrator, **I want to** retrieve a list of all events for a particular product
* **As an** Administrator, **I want to** retrieve a list of all product events between two dates
* **As an** Administrator, **I want to** retrieve a list of all events for a particular product between two dates
* **As an** Administrator, **I want to** retrieve a list of all events for products within a particular category




## Order Service



### User Story O1: Create/get/update Order

* **As a** Customer, **I want to** create an order
* **As a** Customer, **I want to** cancel an order
* **As a** Customer, **I want to** view my orders
* **As a** Customer, **I want to** view one of my orders by supplying its ID



### User Story O2: Get/update Order

* **As an** administrator, **I want to** view all orders for a customer by supplying the customer's ID
* **As an** administrator, **I want to** view all orders for a customer by supplying the customer's email
* **As an** administrator, **I want to** view all orders for a customer by supplying the customer's phone
* **As an** administrator, **I want to** view all orders containing a particular product by supplying the product's ID
* **As an** administrator, **I want to** view an order by supplying the order's ID
* **As an** administrator, **I want to** update an order when its status changes



### User Story O3: Get Order events

* **As an** Administrator, **I want to** retrieve a list of all order events
* **As an** Administrator, **I want to** retrieve a list of all events for a particular order
* **As an** Administrator, **I want to** retrieve a list of all order events between two dates
* **As an** Administrator, **I want to** retrieve a list of all events for a particular order between two dates





## Notification Service



### User Story N1: Create/get Notification

* **As an** administrator, **I want to** create a notification by supplying a customer ID and a message
* **As an** administrator, **I want to** create a notification by supplying a customer email and a message
* **As an** administrator, **I want to** create a notification by supplying a customer phone and a message
* **As an** administrator, **I want to** create a notification for all customers by supplying a message to broadcast
* **As an** administrator, **I want to** create a notification for all customers in a particular area by supplying a postcode, state, or country and a message to broadcast
* **As an** administrator, **I want to** view all notifications for a particular customer by supplying a customer ID
* **As an** administrator, **I want to** view all notifications for a particular customer by supplying a customer email
* **As an** administrator, **I want to** view all notifications for a particular customer by supplying a customer phone
* **As an** administrator, **I want to** view all notifications issued on a particular date
* **As an** administrator, **I want to** view all notifications issued between two dates (inclusive)
* **As an** administrator, **I want to** view a notification by supplying a notification ID

Notifications are recorded with an Email/Phone type; external delivery is not implemented. Order placement, successful cancellation and failed cancellation create notifications asynchronously. Ordinary status updates do not.

## AI Product Recommendations

* **As a** Customer, **I want to** describe product needs in natural language and receive up to five catalogue products.
* **As a** Customer, **I want to** refine recommendations using the same session ID.
* **As a** Customer, **I want to** optionally use a preference summary derived from previous orders.

See [7-AI-agent.md](7-AI-agent.md) for the implemented contract, configuration and limitations. Name-based identity lookup and guaranteed PII redaction are not implemented.

## Proposed Analytics (Not Implemented)

The group report proposes trending-product and order/revenue analytics. These are future work, not delivered API features. Kafka Streams currently maintains each service's read projection.

## Event-Sourcing and CQRS Acceptance Criteria

For successful operations that change persisted state (no-op basket operations and broadcasts to no recipients need not append an event):

- The service appends an immutable domain event before acknowledging success.
- Kafka events have a unique `eventId`, aggregate type and ID, aggregate version, event type, `occurredAt`, nullable correlation/causation IDs, payload and `schemaVersion` (currently 1). REST event-history DTOs do not expose `schemaVersion`.
- Resource queries use service-owned projections except basket reads, which use the Basket table with a CustomerView existence check. Event-history queries read persisted event records.
- Projection-backed query endpoints may temporarily lag after a successful command. Command responses use the status codes documented in the API contract; the current implementation does not return `202 Accepted` solely because a projection is catching up.
- Event consumers are idempotent and tolerate duplicate delivery.
- Projections use aggregate-version checkpoints; cross-service listeners use processed-event IDs.
- Basket-backed checkout clears the basket asynchronously; explicit item-list checkout does not clear it. Ineligible cancellation returns HTTP 200 with the unchanged order status and records `OrderCancelFailedEvent`.



\*\*End of file\*\*
