# E-Commerce Order & Notification Microservice

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

A Spring Boot microservice for placing and retrieving e-commerce orders. Orders are validated and persisted to PostgreSQL, and a decoupled, event-driven notification component informs customers asynchronously once their order has been committed. All errors are returned in a single, consistent JSON format.

---

## Key Features

- **RESTful Order API** — create and retrieve orders, with declarative request validation (Jakarta Bean Validation) and `201 Created` responses that include a `Location` header.
- **Asynchronous, event-driven notifications** — `OrderService` publishes an `OrderCreatedEvent`; `NotificationService` handles it with `@Async` and `@TransactionalEventListener(phase = AFTER_COMMIT)`, so notifications run off the request thread and fire **only after the order is durably saved** — never for a rolled-back transaction.
- **Global exception handling** — a `@RestControllerAdvice` maps every failure (validation, not found, malformed JSON, type mismatch, unsupported method, unexpected errors) to a uniform `ErrorResponse` with the correct HTTP status. Unexpected errors are logged server-side and returned with a safe, generic message.
- **Server-controlled state** — clients cannot set an order's `id` or `status`; new orders always start as `PENDING`.
- **Containerized database** — PostgreSQL runs via Docker Compose with a persistent named volume.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 (Spring MVC, Spring Data JPA, Bean Validation) |
| Persistence | Hibernate ORM 7, PostgreSQL 15 |
| Infrastructure | Docker, Docker Compose |
| Build | Maven (via Maven Wrapper) |
| Tooling | Lombok, SLF4J |

## Architecture & Workflow

```
            HTTP request
                 │
                 ▼
        ┌──────────────────┐        exceptions        ┌────────────────────────┐
        │ OrderController  │ ───────────────────────▶ │ GlobalExceptionHandler │ ──▶ ErrorResponse (JSON)
        └────────┬─────────┘                          └────────────────────────┘
                 │  @Valid request body
                 ▼
        ┌──────────────────┐   save()   ┌─────────────────┐       ┌──────────────┐
        │   OrderService   │ ─────────▶ │ OrderRepository │ ────▶ │  PostgreSQL  │
        │  (@Transactional)│            └─────────────────┘       └──────────────┘
        └────────┬─────────┘
                 │  publishEvent(OrderCreatedEvent)
                 ▼
     ApplicationEventPublisher ── after transaction commit ──▶ ┌─────────────────────┐
                                    (async worker thread)      │ NotificationService │ ──▶ customer notification
                                                               └─────────────────────┘
```

1. **`OrderController`** receives the request; `@Valid` enforces field constraints before any business logic runs.
2. **`OrderService`** applies server-side defaults, saves the order through **`OrderRepository`** (Spring Data JPA) inside a transaction, and publishes an **`OrderCreatedEvent`**.
3. When the transaction **commits**, Spring dispatches the event to **`NotificationService`** on a separate thread (`@EnableAsync`), so the API response is never delayed by notification work.
4. Any exception thrown along the way is translated by **`GlobalExceptionHandler`** into a consistent `ErrorResponse`.

### Project Structure

```
src/main/java/com/ecommerce/order_service
├── OrderServiceApplication.java    # Entry point, @EnableAsync
├── controller/OrderController.java # REST endpoints
├── service/
│   ├── OrderService.java           # Business logic + event publishing
│   └── NotificationService.java    # Async, after-commit event listener
├── repository/OrderRepository.java # Spring Data JPA repository
├── model/Order.java                # JPA entity (orders table)
├── event/OrderCreatedEvent.java    # Immutable domain event (record)
├── dto/ErrorResponse.java          # Uniform error body (record)
└── exception/
    ├── GlobalExceptionHandler.java # @RestControllerAdvice
    └── ResourceNotFoundException.java
```

## Getting Started

### Prerequisites

- **Java 21** (JDK)
- **Docker** with Docker Compose

Maven does not need to be installed — the project ships with the Maven Wrapper.

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts a `postgres:15-alpine` container named `ecommerce-postgres` with database `order_db`, exposed on **host port 5434** (chosen to avoid clashing with a locally installed PostgreSQL on 5432/5433). Data persists in the `postgres_data` volume.

To reset the database completely:

```bash
docker compose down -v
```

### 2. Run the application

```bash
# macOS / Linux
./mvnw spring-boot:run

# Windows (PowerShell)
.\mvnw spring-boot:run
```

The API is available at **http://localhost:8080**. Hibernate creates or updates the `orders` table automatically on startup.

### Configuration

Connection settings live in `src/main/resources/application.properties` and match `docker-compose.yml`:

| Property | Value |
|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5434/order_db` |
| `spring.datasource.username` | `postgres` |
| `spring.datasource.password` | `password123` (local development only) |
| `spring.jpa.hibernate.ddl-auto` | `update` |

## API Reference

Base URL: `http://localhost:8080/api/orders`

| Method | Endpoint | Description | Success | Errors |
|---|---|---|---|---|
| `POST` | `/api/orders` | Create a new order | `201 Created` + `Location` header | `400` validation / malformed JSON |
| `GET` | `/api/orders` | List all orders | `200 OK` | — |
| `GET` | `/api/orders/{id}` | Get a single order by ID | `200 OK` | `404` not found, `400` non-numeric ID |

### Order fields

| Field | Type | Rules |
|---|---|---|
| `customerEmail` | string | Required, valid email address |
| `productName` | string | Required, not blank |
| `quantity` | integer | Required, ≥ 1 |
| `price` | decimal | Required, > 0 |
| `id`, `status` | — | Assigned by the server (`status` starts as `PENDING`) |

### Example: create an order

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerEmail":"jane@example.com","productName":"Laptop","quantity":2,"price":999.99}'
```

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/orders/1
Content-Type: application/json

{"id":1,"customerEmail":"jane@example.com","productName":"Laptop","quantity":2,"price":999.99,"status":"PENDING"}
```

Shortly afterwards, the notification listener logs from a background thread:

```
[task-1] NotificationService : [NOTIFICATION SERVICE] Sending email to jane@example.com: Your order #1 for 'Laptop' has been successfully placed!
```

## Error Handling

Every error response shares the same structure. `validationErrors` appears only when request validation fails.

### 400 Bad Request — validation failure

```json
{
  "timestamp": "2026-10-26T23:01:50.2901361",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "validationErrors": {
    "customerEmail": "must be a well-formed email address",
    "productName": "must not be blank",
    "quantity": "must be greater than or equal to 1",
    "price": "must be greater than 0.00"
  }
}
```

### 404 Not Found — unknown order

```json
{
  "timestamp": "2026-10-26T23:01:50.1128264",
  "status": 404,
  "error": "Not Found",
  "message": "Order with id 9999 was not found"
}
```

### Other handled errors

| Scenario | Status |
|---|---|
| Malformed JSON body | `400 Bad Request` |
| Non-numeric ID (e.g. `/api/orders/abc`) | `400 Bad Request` |
| Unsupported HTTP method | `405 Method Not Allowed` |
| Unknown URL | `404 Not Found` |
| Any unexpected server error | `500 Internal Server Error` (generic message; details logged server-side) |

## Roadmap

- Order status transitions (`PENDING` → `CONFIRMED` → `SHIPPED`) via a `PATCH` endpoint
- Request/response DTOs to decouple the API contract from the JPA entity
- Pagination and filtering for `GET /api/orders`
- Unit and integration tests (`@WebMvcTest`, Testcontainers)
- Real email delivery and a message broker (e.g. Kafka) for cross-service events
- OpenAPI / Swagger UI documentation
