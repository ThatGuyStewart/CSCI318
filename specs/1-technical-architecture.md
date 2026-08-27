# Technical Architecture

## Microservice Architecture

This application contains four **Microservice Modules**:

* **Customer**
* **Product**
* **Order**
* **Notification**



## Layered Architecture

Each microservice module is structured into four distinct layers:

1. **Presentation Layer** (Controllers)
2. **Service Layer** (Business Logic)
3. **Domain Layer** (Core Domain Models \& Rules)
4. **Data Access Layer** (Repositories)



## Repository Structure

The project is structured as a multi-module repository:

* `customer-service`: Module for the Customer Service
* `product-service`: Module for the Product Service
* `order-service`: Module for the Order Service
* `notification-service`: Module for the Notification Service



## Technology Stack

* **JDK \& Build**: Java 21, Apache Maven (Multi-module POM)
* **Framework**: Spring Boot

  * **Controller Layer**: `@RestController`
  * **Service Layer**: `@Service`
  * **Domain Layer**: `@Entity`
  * **Data Access Layer**: `@Repository`
* **Database**: H2 in-memory database for development
* **Inter-Service Compatibility**: DTO to be used for communication between services
* **AI-Powered**: LangChain4j to translate natural language user commands into appropriate api executions
* **Unit \& Integration Testing**: `MockMvc`, `@SpringBootTest`, `JUnit 5`



## Design Principles

* Event oriented, aggregated Domains
* Domain Events handlers within service layer
* Event Sourcing using CQRS



## Future Considerations

Todo



