# Online Retail Microservices Platform

A multi-module Spring Boot microservices platform for an online retail system, built as part of **CSCI318 Assignment 2**.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Services](#services)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Testing](#testing)

---

## Overview

This project implements a distributed **Online Retail Platform** using a microservices architecture. It consists of four independent Spring Boot services that communicate with each other via REST APIs to support customer management, product cataloguing, order processing, and customer notifications.

---

## Architecture

The platform follows a **Layered Microservice Architecture**:

```
+---------------------------------------------------------+
|                  Client / API Consumer                  |
+-------------------+-------------------+-----------------+
                    |                   |
           +--------+------+   +--------+------+   +--------------+
           | Customer Svc  |   | Product Svc   |   |  Order Svc   |
           |   :8081       |   |   :8082       |   |  :8083       |
           +---------------+   +---------------+   +------+-------+
                                                          |
                                                 +--------+----------+
                                                 | Notification Svc  |
                                                 |     :8084         |
                                                 +-------------------+
```

Each microservice is structured into four distinct layers:

| Layer | Annotation | Responsibility |
|---|---|---|
| **Presentation** | `@RestController` | Handles HTTP requests and responses |
| **Service** | `@Service` | Contains business logic |
| **Domain** | `@Entity` | Core domain models and business rules |
| **Data Access** | `@Repository` | Database interaction |

**Inter-service communication** is handled via REST calls using DTOs to maintain loose coupling.

---

## Technology Stack

| Component | Technology |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.16 |
| **Build Tool** | Apache Maven (Multi-module POM) |
| **Database** | H2 (in-memory, for development) |
| **Persistence** | Spring Data JPA |
| **Validation** | Spring Boot Validation |
| **Testing** | JUnit 5, MockMvc, `@SpringBootTest` |

---

## Project Structure

```
retail-platform/
+-- pom.xml                        # Parent multi-module POM
+-- customer-service/              # Customer & Basket management (port 8081)
|   +-- src/main/java/com/retail/customer/
|       +-- controller/
|       +-- service/
|       +-- domain/
|       +-- repository/
|       +-- dto/
|       +-- client/
|       +-- exception/
+-- product-service/               # Product catalogue (port 8082)
|   +-- src/main/java/com/retail/product/
|       +-- controller/
|       +-- service/
|       +-- domain/
|       +-- repository/
|       +-- dto/
|       +-- exception/
+-- order-service/                 # Order processing (port 8083)
|   +-- src/main/java/com/retail/order/
|       +-- controller/
|       +-- service/
|       +-- domain/
|       +-- repository/
|       +-- dto/
|       +-- client/
|       +-- exception/
+-- notification-service/          # Customer notifications (port 8084)
|   +-- src/main/java/com/retail/notification/
|       +-- controller/
|       +-- service/
|       +-- domain/
|       +-- repository/
|       +-- dto/
|       +-- client/
|       +-- exception/
+-- specs/                         # Project specifications
|   +-- 1-technical-architecture.md
|   +-- 2-user-stories.md
|   +-- 3-domain-models.md
|   +-- 4-api-endpoints.md
|   +-- openapi-contract.yaml
+-- api-examples.txt               # cURL and PowerShell API examples
+-- populate-data.ps1              # PowerShell script to seed test data
+-- populate-data.bat              # Batch script to seed test data
+-- start-all.ps1                  # Start all services (PowerShell)
+-- start-all.bat                  # Start all services (Batch)
+-- start-all.sh                   # Start all services (Bash)
+-- stop-all.ps1                   # Stop all services (PowerShell)
+-- stop-all.bat                   # Stop all services (Batch)
+-- stop-all.sh                    # Stop all services (Bash)
```

---

## Services

### 1. Customer Service — `http://localhost:8081`

Manages customer records and shopping baskets.

- Create, read, update, and delete customer records
- Lookup customers by ID, email, or phone
- View and manage a customer's shopping basket
- Validates product existence via inter-service call to Product Service when adding to basket

### 2. Product Service — `http://localhost:8082`

Manages the product catalogue.

- Create, update, and delete products
- List all products or filter by category
- Search products by name (partial match)
- Retrieve a single product by ID

### 3. Order Service — `http://localhost:8083`

Handles order creation, tracking, and lifecycle management.

- Create orders from a customer's basket or from an explicit product + quantity
- Cancel orders in `Placed` or `Pending` status
- Update order status
- Query orders by customer (ID, email, phone) or by product
- Automatically triggers notifications via Notification Service on order create/cancel

### 4. Notification Service — `http://localhost:8084`

Records and retrieves customer notifications.

- Create targeted notifications by customer ID, email, or phone
- Broadcast notifications to all customers or customers in a specific area (postcode, state, or country)
- Query notifications by customer, by date, or by date range

---

## Getting Started

### Prerequisites

- **Java 21** or later
- **Apache Maven 3.8+**

### Running the Services

**Windows (PowerShell):**
```powershell
.\start-all.ps1
```

**Windows (Command Prompt):**
```bat
start-all.bat
```

**macOS / Linux:**
```bash
./start-all.sh
```

Once started, the services are accessible at:

| Service | URL |
|---|---|
| Customer Service | http://localhost:8081 |
| Product Service | http://localhost:8082 |
| Order Service | http://localhost:8083 |
| Notification Service | http://localhost:8084 |

### Running a Single Service

```bash
mvn -pl customer-service spring-boot:run
mvn -pl product-service spring-boot:run
mvn -pl order-service spring-boot:run
mvn -pl notification-service spring-boot:run
```

### Seeding Test Data

After all services are running, populate sample data using:

```powershell
# PowerShell
.\populate-data.ps1
```

```bat
REM Batch
populate-data.bat
```

### Stopping the Services

**Windows (PowerShell):**
```powershell
.\stop-all.ps1
```

**Windows (Command Prompt):**
```bat
stop-all.bat
```

**macOS / Linux:**
```bash
./stop-all.sh
```

---

## API Reference

A full API reference is available in [`specs/4-api-endpoints.md`](specs/4-api-endpoints.md).  
The complete OpenAPI contract is in [`specs/openapi-contract.yaml`](specs/openapi-contract.yaml).  
Practical cURL and PowerShell examples for every endpoint are in [`api-examples.txt`](api-examples.txt).

### Customer Service (`http://localhost:8081`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/customer` | Create a new customer |
| `GET` | `/customer` | List all customers |
| `GET` | `/customer/{id}` | Get customer by ID |
| `GET` | `/customer/email/{email}` | Get customer by email |
| `GET` | `/customer/phone/{phone}` | Get customer by phone |
| `GET` | `/customer/state/{state}` | List customers by state |
| `GET` | `/customer/country/{country}` | List customers by country |
| `GET` | `/customer/postcode/{postcode}` | List customers by postcode |
| `PUT` | `/customer/{id}` | Update customer details |
| `PUT` | `/customer/{id}/address` | Update customer address |
| `DELETE` | `/customer/{id}` | Delete a customer |
| `GET` | `/customer/{id}/basket` | View customer basket |
| `POST` | `/customer/{id}/basket/items` | Add item to basket |
| `DELETE` | `/customer/{id}/basket/items` | Remove item from basket |
| `GET` | `/customer/event` | List all customer domain events |
| `GET` | `/customer/{id}/event` | Customer events by ID |
| `GET` | `/customer/email/{email}/event` | Customer events by email |
| `GET` | `/customer/phone/{phone}/event` | Customer events by phone |

### Product Service (`http://localhost:8082`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/product` | Create a new product |
| `PUT` | `/product/{id}` | Update a product |
| `DELETE` | `/product/{id}` | Delete a product |
| `GET` | `/product` | List all products |
| `GET` | `/product/{id}` | Get product by ID |
| `GET` | `/product/category/{category}` | List products by category |
| `GET` | `/product/search?name={name}` | Search products by name |
| `GET` | `/product/event` | List all product domain events |
| `GET` | `/product/{id}/event` | Product events by ID |
| `GET` | `/product/category/{category}/event` | Product events by category |

### Order Service (`http://localhost:8083`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/order` | Create an order |
| `POST` | `/order/{id}/cancel` | Cancel an order |
| `GET` | `/order/{id}` | Get order by ID |
| `GET` | `/order/customer/{customerId}` | Get orders by customer ID |
| `GET` | `/order/customer/email/{email}` | Get orders by customer email |
| `GET` | `/order/customer/phone/{phone}` | Get orders by customer phone |
| `GET` | `/order/product/{productId}` | Get orders containing a product |
| `PUT` | `/order/{id}/status` | Update order status |
| `GET` | `/order/customer/{customerId}/event` | Order events by customer ID |
| `GET` | `/order/{id}/event` | Order events by order ID |
| `GET` | `/order/customer/email/{email}/event` | Order events by customer email |
| `GET` | `/order/customer/phone/{phone}/event` | Order events by customer phone |
| `GET` | `/order/product/{productId}/event` | Order events by product |

### Notification Service (`http://localhost:8084`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/notification/customer/{customerId}` | Create notification by customer ID |
| `POST` | `/notification/customer/email/{email}` | Create notification by email |
| `POST` | `/notification/customer/phone/{phone}` | Create notification by phone |
| `POST` | `/notification/broadcast` | Broadcast to all customers |
| `POST` | `/notification/broadcast/area` | Broadcast to customers in an area |
| `GET` | `/notification/customer/{customerId}` | Get notifications by customer ID |
| `GET` | `/notification/customer/email/{email}` | Get notifications by customer email |
| `GET` | `/notification/customer/phone/{phone}` | Get notifications by customer phone |
| `GET` | `/notification/date/{date}` | Get notifications by date |
| `GET` | `/notification/date-range?from={from}&to={to}` | Get notifications in a date range |
| `GET` | `/notification/{id}` | Get notification by ID |

---

## Testing

Run all tests from the project root:

```bash
mvn clean test
```

Run tests for a specific module:

```bash
mvn -pl customer-service clean test
mvn -pl product-service clean test
mvn -pl order-service clean test
mvn -pl notification-service clean test
```

Tests use `MockMvc` and `@SpringBootTest` with an in-memory H2 database, covering both happy-path and error scenarios (HTTP 400, 404, etc.) derived from the OpenAPI contract.

---

## Inter-Service Dependencies

```
Order Service       ────► Customer Service
Order Service       ────► Product Service
Order Service       ────► Notification Service
Customer Service    ────► Product Service  (basket item validation)
Notification Svc    ────► Customer Service
```
