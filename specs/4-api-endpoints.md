# API Endpoints Summary

## 1\. Customer Service (`http://localhost:8081`)

|Feature|Method|Endpoint Path|Request Body|Response (Success)|Inter-Service REST Dependencies|Description|
|-|-|-|-|-|-|-|
|**C1**|`POST`|`/customer`|`CustomerCreateRequest`|`201 Created` (`CustomerResponse`)|*None*|Create a new customer record|
|**C2**|`GET`|`/customer`|*None*|`200 OK` (`Array<CustomerResponse>`)|*None*|List all customer records|
|**C1 / C2**|`GET`|`/customer/{id}`|*None*|`200 OK` (`CustomerResponse`)|*None*|Get customer details by id|
|**C1 / C2**|`GET`|`/customer/email/{email}`|*None*|`200 OK` (`CustomerResponse`)|*None*|Get customer details by email|
|**C1 / C2**|`GET`|`/customer/phone/{phone}`|*None*|`200 OK` (`CustomerResponse`)|*None*|Get customer details by phone|
|**C1**|`PUT`|`/customer/{id}`|`CustomerUpdateRequest`|`200 OK` (`CustomerResponse`)|*None*|Update customer details by id|
|**C1**|`PUT`|`/customer/{id}/address`|`AddressUpdateRequest`|`200 OK` (`AddressResponse`)|*None*|Update customer address by id|
|**C1**|`DELETE`|`/customer/{id}`|*None*|`204 No Content`|*None*|Delete a customer record by id|
|**C3**|`GET`|`/customer/{id}/basket`|*None*|`200 OK` (`BasketResponse`)|*None*|View a customer's basket|
|**C3**|`POST`|`/customer/{id}/basket/items`|`BasketAddRequest`|`200 OK` (`BasketResponse`)|**Product Service:** `GET /product/{id}`|Add a quantity of a product to a customer's basket|
|**C3**|`DELETE`|`/customer/{id}/basket/items`|`BasketRemoveRequest`|`200 OK` (`BasketResponse`)|*None*|Remove a quantity of a product from a customer's basket|
|**C4**|`GET`|`/customer/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<CustomerDomainEventResponse>`)|*None*|View all customer domain events, with option to filter by date or date range (inclusive)|
|**C4**|`GET`|`/customer/{id}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<CustomerDomainEventResponse>`)|*None*|View all customer domain events for a customer by id, with option to filter by date or date range (inclusive)|
|**C4**|`GET`|`/customer/email/{email}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`CustomerDomainEventResponse`)|*None*|View all customer domain events for a customer by email, with option to filter by date or date range (inclusive)|
|**C4**|`GET`|`/customer/phone/{phone}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`CustomerDomainEventResponse`)|*None*|View all customer domain events for a customer by phone, with option to filter by date or date range (inclusive)|

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
|**P3**|`GET`|`/product/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<ProductDomainEventResponse>`)|*None*|View all product domain events, with option to filter by date or date range (inclusive)|
|**P3**|`GET`|`/product/{id}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<ProductDomainEventResponse>`)|*None*|View all product domain events for a product, with option to filter by date or date range (inclusive)|
|**P3**|`GET`|`/product/category/{category}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<ProductDomainEventResponse>`)|*None*|List all product domain events for products in a given category, with option to filter by date or date range (inclusive)|

\---

## 3\. Order Service (`http://localhost:8083`)

|Feature|Method|Endpoint Path|Request Body|Response (Success)|Inter-Service REST Dependencies|Description|
|-|-|-|-|-|-|-|
|**O1**|`POST`|`/order`|`OrderCreateRequest`|`201 Created` (`OrderResponse`)|**Customer Service:** `GET /customer/{id}`; **Product Service:** `GET /product/{id}`; **Notification Service:** `POST /notification`|Create an order (from basket or explicit product/quantity)|
|**O1**|`POST`|`/order/{id}/cancel`|*None*|`200 OK` (`OrderResponse`)|**Notification Service:** `POST /notification`|Cancel an order (if status is `Placed` or `Pending`)|
|**O1**|`GET`|`/order/customer/{customerId}`|*None*|`200 OK` (`Array<OrderResponse>`)|*None*|View all orders for the authenticated customer|
|**O1**|`GET`|`/order/{id}`|*None*|`200 OK` (`OrderResponse`)|*None*|View a specific order by id|
|**O2**|`GET`|`/order/customer/{customerId}`|*None*|`200 OK` (`Array<OrderResponse>`)|*None*|View all orders by customer id|
|**O2**|`GET`|`/order/customer/email/{email}`|*None*|`200 OK` (`Array<OrderResponse>`)|**Customer Service:** `GET /customer/email/{email}`|View all orders by customer email|
|**O2**|`GET`|`/order/customer/phone/{phone}`|*None*|`200 OK` (`Array<OrderResponse>`)|**Customer Service:** `GET /customer/phone/{phone}`|View all orders by customer phone|
|**O2**|`GET`|`/order/product/{productId}`|*None*|`200 OK` (`Array<OrderResponse>`)|*None*|View all orders containing a product|
|**O2**|`PUT`|`/order/{id}/status`|`OrderStatusUpdateRequest`|`200 OK` (`OrderResponse`)|**Notification Service:** `POST /notification` (optional)|Update order status|
|**O1**|`GET`|`/order/customer/{customerId}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<OrderDomainEventResponse>`)|*None*|View all order domain events for the authenticated customer, with option to filter by date or date range (inclusive)|
|**O1**|`GET`|`/order/{id}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`OrderDomainEventResponse`)|*None*|View all order domain events for a specific order by id, with option to filter by date or date range (inclusive)|
|**O2**|`GET`|`/order/customer/{customerId}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<OrderDomainEventResponse>`)|*None*|View all order domain events for orders by customer id, with option to filter by date or date range (inclusive)|
|**O2**|`GET`|`/order/customer/email/{email}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<OrderDomainEventResponse>`)|**Customer Service:** `GET /customer/email/{email}`|View all order domain events for orders by customer email, with option to filter by date or date range (inclusive)|
|**O2**|`GET`|`/order/customer/phone/{phone}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<OrderDomainEventResponse>`)|**Customer Service:** `GET /customer/phone/{phone}`|View all order domain events for orders by customer phone, with option to filter by date or date range (inclusive)|
|**O2**|`GET`|`/order/product/{productId}/event`|Optional Query params `date`, or `from` and `to`|`200 OK` (`Array<OrderDomainEventResponse>`)|*None*|View all order domain events for orders containing a product, with option to filter by date or date range (inclusive)|

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
|**N1**|`GET`|`/notification/{id}`|*None*|`200 OK` (`NotificationResponse`)|*None*|View a notification by id|



