# API Endpoints Summary

## CQRS and Event-Sourcing Contract

`POST`, `PUT`, and `DELETE` operations are commands. A command validates its input, appends immutable domain events to the owning service's event store, and publishes those events to Kafka. `GET` operations are queries: they read service-owned projections only and do not invoke command handlers or write events.

Read projections are eventually consistent with accepted commands. Event-query endpoints return persisted `DomainEventEnvelope` records rather than events reconstructed from the current state of an aggregate.

|Service|Publishes|Consumes|
|-|-|-|
|Customer Service|`customer.events`|`product.events`, `order.events`|
|Product Service|`product.events`|None|
|Order Service|`order.events`|`customer.events` when needed to update its projection|
|Notification Service|`notification.events`|`order.events`|

## 1\. Customer Service (`http://localhost:8081`)

|Feature|Method|Endpoint Path|Request Body|Response (Success)|Inter-Service REST Dependencies|Description|
|-|-|-|-|-|-|-|
|**C1**|`POST`|`/customer`|`CustomerCreateRequest`|`201 Created` (`CustomerResponse`)|*None*|Create a new customer record|
|**C2**|`GET`|`/customer`|*None*|`200 OK` (`Array<CustomerResponse>`)|*None*|List all customer records|
|**C1 / C2**|`GET`|`/customer/{id}`|*None*|`200 OK` (`CustomerResponse`)|*None*|Get customer details by id|
|**C1 / C2**|`GET`|`/customer/email/{email}`|*None*|`200 OK` (`CustomerResponse`)|*None*|Get customer details by email|
|**C1 / C2**|`GET`|`/customer/phone/{phone}`|*None*|`200 OK` (`CustomerResponse`)|*None*|Get customer details by phone|
|**C2**|`GET`|`/customer/state/{state}`|*None*|`200 OK` (`Array<CustomerResponse>`)|*None*|List customer projections by state|
|**C2**|`GET`|`/customer/country/{country}`|*None*|`200 OK` (`Array<CustomerResponse>`)|*None*|List customer projections by country|
|**C2**|`GET`|`/customer/postcode/{postcode}`|*None*|`200 OK` (`Array<CustomerResponse>`)|*None*|List customer projections by postcode|
|**C1**|`PUT`|`/customer/{id}`|`CustomerUpdateRequest`|`200 OK` (`CustomerResponse`)|*None*|Update customer details by id|
|**C1**|`PUT`|`/customer/{id}/address`|`AddressUpdateRequest`|`200 OK` (`AddressResponse`)|*None*|Update customer address by id|
|**C1**|`DELETE`|`/customer/{id}`|*None*|`204 No Content`|*None*|Delete a customer record by id|
|**C3**|`GET`|`/customer/{id}/basket`|*None*|`200 OK` (`BasketResponse`)|*None*|View a customer's basket|
|**C3**|`POST`|`/customer/{id}/basket`|`BasketAddRequest`|`200 OK` (`BasketResponse`)|**Product Service:** `GET /product/{id}`|Add a quantity of a product to a customer's basket|
|**C3**|`DELETE`|`/customer/{id}/basket`|`BasketRemoveRequest`|`200 OK` (`BasketResponse`)|*None*|Remove a quantity of a product from a customer's basket|
|**C4**|`GET`|`/customer/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|*None*|Read persisted customer events, optionally filtered by an inclusive date range|
|**C4**|`GET`|`/customer/{id}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|*None*|Read persisted customer events by aggregate id|
|**C4**|`GET`|`/customer/email/{email}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|*None*|Read persisted customer events by email|
|**C4**|`GET`|`/customer/phone/{phone}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|*None*|Read persisted customer events by phone|

\---

## 2\. Product Service (`http://localhost:8082`)

|Feature|Method|Endpoint Path|Request Body|Response (Success)|Inter-Service REST Dependencies|Description|
|-|-|-|-|-|-|-|
|**P1**|`POST`|`/product`|`ProductCreateRequest`|`201 Created` (`ProductResponse`)|*None*|Create a new product|
|**P1**|`PUT`|`/product/{id}`|`ProductUpdateRequest`|`200 OK` (`ProductResponse`)|*None*|Update a product by id|
|**P1**|`DELETE`|`/product/{id}`|*None*|`204 No Content`|*None*|Delete a product by id|
|**P2**|`GET`|`/product`|*None*|`200 OK` (`Array<ProductResponse>`)|*None*|List all products|
|**P2**|`GET`|`/product/{id}`|*None*|`200 OK` (`ProductResponse`)|*None*|Get product details by id|
|**P2**|`GET`|`/product/category/{category}`|*None*|`200 OK` (`Array<ProductResponse>`)|*None*|List products in a given category|
|**P2**|`GET`|`/product/search`|Query param `name`|`200 OK` (`Array<ProductResponse>`)|*None*|Search products by name (partial match)|
|**P3**|`GET`|`/product/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|*None*|Read persisted product events, optionally filtered by an inclusive date range|
|**P3**|`GET`|`/product/{id}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|*None*|Read persisted product events by aggregate id|
|**P3**|`GET`|`/product/category/{category}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|*None*|Read persisted product events by category|

\---

## 3\. Order Service (`http://localhost:8083`)

|Feature|Method|Endpoint Path|Request Body|Response (Success)|Inter-Service REST Dependencies|Description|
|-|-|-|-|-|-|-|
|**O1**|`POST`|`/order`|`OrderCreateRequest`|`201 Created` (`OrderResponse`)|**Customer Service:** `GET /customer/{id}`; **Product Service:** `GET /product/{id}`; publishes `OrderPlacedEvent` to `order.events`|Create an order (from basket or explicit product/quantity)|
|**O1**|`POST`|`/order/{id}/cancel`|*None*|`200 OK` (`OrderResponse`)|publishes `OrderCancelledEvent` to `order.events`|Cancel an order (if status is `Placed` or `Pending`)|
|**O1**|`GET`|`/order/customer/{customerId}`|*None*|`200 OK` (`Array<OrderResponse>`)|*None*|View all orders for the authenticated customer|
|**O1**|`GET`|`/order/{id}`|*None*|`200 OK` (`OrderResponse`)|*None*|View a specific order by id|
|**O2**|`GET`|`/order/customer/{customerId}`|*None*|`200 OK` (`Array<OrderResponse>`)|*None*|View all orders by customer id|
|**O2**|`GET`|`/order/customer/email/{email}`|*None*|`200 OK` (`Array<OrderResponse>`)|**Customer Service:** `GET /customer/email/{email}`|View all orders by customer email|
|**O2**|`GET`|`/order/customer/phone/{phone}`|*None*|`200 OK` (`Array<OrderResponse>`)|**Customer Service:** `GET /customer/phone/{phone}`|View all orders by customer phone|
|**O2**|`GET`|`/order/product/{productId}`|*None*|`200 OK` (`Array<OrderResponse>`)|*None*|View all orders containing a product|
|**O2**|`PUT`|`/order/{id}/status`|`OrderStatusUpdateRequest`|`200 OK` (`OrderResponse`)|publishes `OrderStatusChangedEvent` to `order.events`|Update order status|
|**O3**|`GET`|`/order/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|*None*|Read all persisted order events, optionally filtered by an inclusive date range|
|**O1**|`GET`|`/order/customer/{customerId}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|*None*|Read persisted order events for the authenticated customer|
|**O1**|`GET`|`/order/{id}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|*None*|Read persisted order events by aggregate id|
|**O2**|`GET`|`/order/customer/{customerId}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|*None*|Read persisted order events by customer id|
|**O2**|`GET`|`/order/customer/email/{email}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|**Customer Service:** `GET /customer/email/{email}`|Read persisted order events by customer email|
|**O2**|`GET`|`/order/customer/phone/{phone}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|**Customer Service:** `GET /customer/phone/{phone}`|Read persisted order events by customer phone|
|**O2**|`GET`|`/order/product/{productId}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<DomainEventEnvelope>`)|*None*|Read persisted order events by product id|

\---

## 4\. Notification Service (`http://localhost:8084`)

|Feature|Method|Endpoint Path|Request Body|Response (Success)|Inter-Service REST Dependencies|Description|
|-|-|-|-|-|-|-|
|**N1**|`POST`|`/notification/customer/{customerId}`|`NotificationCreateRequest`|`201 Created` (`NotificationResponse`)|**Customer Service:** `GET /customer/{id}`|Create notification by customer id|
|**N1**|`POST`|`/notification/customer/email/{email}`|`NotificationCreateRequest`|`201 Created` (`NotificationResponse`)|**Customer Service:** `GET /customer/email/{email}`|Create notification by customer email|
|**N1**|`POST`|`/notification/customer/phone/{phone}`|`NotificationCreateRequest`|`201 Created` (`NotificationResponse`)|**Customer Service:** `GET /customer/phone/{phone}`|Create notification by customer phone|
|**N1**|`POST`|`/notification/broadcast`|`NotificationBroadcastRequest`|`201 Created` (`Array<NotificationResponse>`)|**Customer Service:** `GET /customer`|Broadcast notification to all customers|
|**N1**|`POST`|`/notification/broadcast/area`|`NotificationAreaBroadcastRequest`|`201 Created` (`Array<NotificationResponse>`)|**Customer Service:** `GET /customer` (filtered by postcode/state/country)|Broadcast notification to customers in an area|
|**N1**|`GET`|`/notification/customer/{customerId}`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<NotificationResponse>`)|*None*|View notifications by customer id, with option to filter by date or date range (inclusive)|
|**N1**|`GET`|`/notification/customer/email/{email}`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<NotificationResponse>`)|**Customer Service:** `GET /customer/email/{email}`|View notifications by customer email, with option to filter by date or date range (inclusive)|
|**N1**|`GET`|`/notification/customer/phone/{phone}`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<NotificationResponse>`)|**Customer Service:** `GET /customer/phone/{phone}`|View notifications by customer phone, with option to filter by date or date range (inclusive)|
|**N1**|`GET`|`/notification`|Optional Query params `date`, or `from` and `to` |`200 OK` (`Array<NotificationResponse>`)|*None*|View all notifications, with option to filter by date or date range (inclusive)|
|**N1**|`GET`|`/notification/date/{date}`|*None*|`200 OK` (`Array<NotificationResponse>`)|*None*|View notifications issued on a date|
|**N1**|`GET`|`/notification/date-range`|Required query params `from`, `to`|`200 OK` (`Array<NotificationResponse>`)|*None*|View notifications issued within an inclusive date range|
|**N1**|`GET`|`/notification/{id}`|*None*|`200 OK` (`NotificationResponse`)|*None*|View a notification by id|



