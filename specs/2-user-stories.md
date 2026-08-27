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
* **As an** administrator, **I want to** create a notification for all customers by supplying "broadcast" (as the customer ID), and the message to broadcast
* **As an** administrator, **I want to** create a notification for all customers in a particular area by supplying "broadcast" (as the customer ID), a postcode, state, or Country to broadcast to, and the message to broadcast
* **As an** administrator, **I want to** view all notifications for a particular customer by supplying a customer ID
* **As an** administrator, **I want to** view all notifications for a particular customer by supplying a customer email
* **As an** administrator, **I want to** view all notifications for a particular customer by supplying a customer phone
* **As an** administrator, **I want to** view all notifications issued on a particular date
* **As an** administrator, **I want to** view all notifications issued between two dates (inclusive)
* **As an** administrator, **I want to** view a notification by supplying a notification ID







\*\*End of file\*\*



