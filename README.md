# AirBnb Backend

A hotel booking platform backend — a monolithic Spring Boot application handling real-time inventory management, dynamic pricing, Stripe payment integration, and a full booking state machine, all running as a single deployable service.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [High-Level Architecture](#2-high-level-architecture)
3. [Repository Structure](#3-repository-structure)
4. [Application Layers](#4-application-layers)
   - 4.1 [Controllers](#41-controllers)
   - 4.2 [Services](#42-services)
   - 4.3 [Repositories](#43-repositories)
   - 4.4 [Security](#44-security)
   - 4.5 [Pricing Strategy Layer](#45-pricing-strategy-layer)
   - 4.6 [Exception Handling and Advice](#46-exception-handling-and-advice)
   - 4.7 [Configuration](#47-configuration)
5. [Data Layer](#5-data-layer)
   - 5.1 [PostgreSQL Schema](#51-postgresql-schema)
   - 5.2 [Entity Relationships](#52-entity-relationships)
6. [Core Domain Entities](#6-core-domain-entities)
7. [Business Logic Deep Dive](#7-business-logic-deep-dive)
   - 7.1 [Booking Flow](#71-booking-flow)
   - 7.2 [Dynamic Pricing Engine](#72-dynamic-pricing-engine)
   - 7.3 [Inventory Management](#73-inventory-management)
   - 7.4 [Payment and Refunds](#74-payment-and-refunds)
8. [API Documentation](#8-api-documentation)
9. [Security and Authentication](#9-security-and-authentication)
10. [Scheduled Jobs](#10-scheduled-jobs)
11. [Advanced Features and Design Decisions](#11-advanced-features-and-design-decisions)
12. [Challenges and Solutions](#12-challenges-and-solutions)
13. [Technology Stack](#13-technology-stack)
14. [Configuration Management](#14-configuration-management)
15. [Getting Started](#15-getting-started)
16. [Acknowledgments](#16-acknowledgments)
17. [Contact](#17-contact)
18. [Project Stats and Learning Outcomes](#18-project-stats-and-learning-outcomes)

---

## 1. Project Overview

This is a full-featured hotel booking backend built with Spring Boot, designed to handle complex scenarios like concurrent bookings, real-time inventory management, dynamic pricing, and Stripe payment processing. The system implements sophisticated business logic including pessimistic locking for race condition prevention, state-machine-based booking workflows, and intelligent refund policies.

Unlike a distributed system, this project is a single monolithic application: one deployable Spring Boot service, one database, and no inter-service communication. All modules (auth, hotels, bookings, pricing, payments, notifications) live in one codebase and share a single transactional boundary.

[Back to top](#table-of-contents)

---

## 2. High-Level Architecture

```
┌─────────────────┐
│   Client (Web)  │
└────────┬────────┘
         │ HTTPS
         ▼
┌─────────────────┐
│  Spring Boot    │◄──────┐
│   Controllers   │       │
└────────┬────────┘       │
         │                │
┌────────▼────────┐       │
│  Service Layer  │       │
│  (Business      │       │
│   Logic)        │       │
└────────┬────────┘       │
         │                │
    ┌────┴────┐           │
    │         │           │
┌───▼──┐  ┌── ▼───┐       │
│ JPA  │  │Stripe │       │
│Repos │  │  API  │       │
└───┬──┘  └────── ┘       │
    │                     │
┌───▼──────┐    ┌──────── ┴────────┐
│PostgreSQL│    │  Scheduled Jobs  │
│ Database │    │  - Pricing       │
└──────────┘    │  - Cleanup       │
                └───────────────── ┘
```

The application follows a classic layered monolith design: controllers accept HTTP requests, delegate to a service layer that contains all business logic, and the service layer talks to JPA repositories (PostgreSQL) and external APIs (Stripe, Calendarific). Scheduled jobs run in-process alongside the web server to handle pricing recalculation and expired-booking cleanup.

[Back to top](#table-of-contents)

---

## 3. Repository Structure

```
AIRBNB-BACKEND/
├── src/
│   ├── main/
│   │   ├── java/com/nikhil/airbnb/
│   │   │   ├── advice/                    # Global exception handling (@ControllerAdvice)
│   │   │   ├── config/                    # Spring, Stripe, CORS, Swagger configuration
│   │   │   ├── controller/                # REST controllers (public, user, admin, webhook)
│   │   │   ├── dto/                       # Request/response DTOs
│   │   │   ├── entity/                    # JPA entities
│   │   │   │   └── enums/                 # Domain enums (BookingStatus, Role, Gender, etc.)
│   │   │   ├── exception/                 # Custom exception types
│   │   │   ├── handler/                   # Webhook and event handlers
│   │   │   ├── repository/                # Spring Data JPA repositories
│   │   │   ├── security/                  # JWT filters, UserDetails, Spring Security config
│   │   │   ├── service/
│   │   │   │   ├── serviceInterfaces/     # Service contracts
│   │   │   │   └── serviceImplementations/# Business logic implementations
│   │   │   ├── strategy/                  # Dynamic pricing strategy implementations
│   │   │   └── util/                      # Shared helper utilities
│   │   └── resources/
│   │       ├── static/
│   │       └── templates/                 # Thymeleaf email templates
│   └── test/
│       └── java/com/nikhil/airbnb/
├── .mvn/wrapper/
├── pom.xml
├── .gitattributes
├── .gitignore
└── README.md
```

Being a monolith, the entire application is a single Maven module. Layering is enforced by package boundaries (`controller` → `service` → `repository`) rather than by separate deployable artifacts.

[Back to top](#table-of-contents)

---

## 4. Application Layers

### 4.1 Controllers

**Technology:** Spring Web (MVC)

REST controllers are split by access level: public endpoints (search, hotel details), authenticated user endpoints (bookings, guests), admin endpoints gated by the `HOTEL_MANAGER` role (hotel and room management, inventory, reports), and a dedicated Stripe webhook controller. Controllers are intentionally thin — they validate input via DTOs and delegate all logic to the service layer.

[Back to Application Layers](#4-application-layers) | [Back to top](#table-of-contents)

---

### 4.2 Services

**Technology:** Spring Data JPA, Spring Transactions

The service layer is split into interfaces (`serviceInterfaces`) and implementations (`serviceImplementations`), keeping business logic behind stable contracts. This is where the booking state machine, dynamic pricing orchestration, inventory locking, and refund calculations all live. Services are annotated with `@Transactional` at the concrete implementation level so that multi-step operations (e.g., reserving inventory and creating a booking) commit or roll back atomically.

[Back to Application Layers](#4-application-layers) | [Back to top](#table-of-contents)

---

### 4.3 Repositories

**Technology:** Spring Data JPA, Hibernate

Repositories expose both derived query methods and custom `@Query`/`@Modifying` methods for atomic updates (e.g., incrementing `reservedCount`, decrementing `bookedCount`). Several repositories use `@Lock(LockModeType.PESSIMISTIC_WRITE)` to serialize concurrent access to inventory rows during booking.

[Back to Application Layers](#4-application-layers) | [Back to top](#table-of-contents)

---

### 4.4 Security

**Technology:** Spring Security, JJWT, BCrypt

`AppUser` implements Spring Security's `UserDetails` interface directly, using email as the username and BCrypt-hashed passwords. JWT access and refresh tokens are issued on login, with the refresh token stored in an HttpOnly cookie. Role-based access control (`USER`, `HOTEL_MANAGER`) gates admin endpoints.

[Back to Application Layers](#4-application-layers) | [Back to top](#table-of-contents)

---

### 4.5 Pricing Strategy Layer

**Technology:** Decorator Pattern

The `strategy` package implements the dynamic pricing engine as a chain of decorators — base pricing, surge pricing, urgency pricing, holiday pricing, and occupancy pricing — each wrapping the previous strategy and applying its own multiplier. This is detailed further in [Dynamic Pricing Engine](#72-dynamic-pricing-engine).

[Back to Application Layers](#4-application-layers) | [Back to top](#table-of-contents)

---

### 4.6 Exception Handling and Advice

**Technology:** `@ControllerAdvice`

The `advice` package centralizes exception-to-HTTP-response mapping using a global `@ControllerAdvice` handler, while the `exception` package defines domain-specific exception types (e.g., `InventoryModifiedException`, `InvalidStateTransitionException`, `BookingNotFoundException`).

[Back to Application Layers](#4-application-layers) | [Back to top](#table-of-contents)

---

### 4.7 Configuration

**Technology:** Spring `@Configuration`

The `config` package holds Spring Security configuration, Stripe client setup, CORS configuration, and SpringDoc OpenAPI (Swagger) configuration. All environment-specific values are externalized via `application-local.yml` and environment variables rather than hardcoded here.

[Back to Application Layers](#4-application-layers) | [Back to top](#table-of-contents)

---

## 5. Data Layer

### 5.1 PostgreSQL Schema

A single PostgreSQL database (`airbnb_db`) backs the entire application. Hibernate manages schema generation via `ddl-auto: update` in local development (a proper migration tool such as Flyway or Liquibase is recommended for production).

| Table | Owned By | Notes |
|---|---|---|
| `app_user` | Auth / Users | Implements `UserDetails`; roles stored via `@ElementCollection` |
| `hotel` | Hotels | Soft delete via `active` flag; images/amenities as element collections |
| `room` | Rooms | Free-text `type` field; independent inventory per room |
| `inventory` | Inventory | Three-tier count system (`total`, `booked`, `reserved`); unique per hotel/room/date |
| `booking` | Bookings | State machine driven; unique `payment_session_id` |
| `guest` | Guests | Reusable guest profiles per user |
| `holidays` | Pricing | Composite key `(date, country_code)`; sourced from Calendarific |
| `hotel_min_prices` | Search optimization | Cached minimum price per hotel/date |

[Back to Data Layer](#5-data-layer) | [Back to top](#table-of-contents)

---

### 5.2 Entity Relationships

```
AppUser (1) ────< (N) Hotel [as owner]
AppUser (1) ────< (N) Booking [as user]
AppUser (1) ────< (N) Guest [as owner]

Hotel (1) ────< (N) Room
Hotel (1) ────< (N) Inventory
Hotel (1) ────< (N) Booking
Hotel (1) ────< (N) HotelMinPrice

Room (1) ────< (N) Inventory
Room (1) ────< (N) Booking

Booking (N) ────< (M) Guest [via booking_guests join table]

Holiday [Composite PK: (date, countryCode)]
```

[Back to Data Layer](#5-data-layer) | [Back to top](#table-of-contents)

---

## 6. Core Domain Entities

**AppUser** — implements `UserDetails` for Spring Security integration. `getUsername()` returns email; `getAuthorities()` converts stored roles into `ROLE_`-prefixed authorities. Owns hotels, bookings, and guest profiles.

**Hotel** — owned by an `AppUser` manager. Holds an embedded `HotelContactInfo` object (address, phone, email, location string). Soft-deleted via an `active` flag rather than a hard delete, which prevents new bookings without destroying booking history.

**Room** — belongs to a `Hotel`. `type` is a free-text field so managers can define arbitrary categories (e.g., "Deluxe Suite", "Ocean View"). Creating a room automatically generates 365 days of `Inventory` rows ahead.

**Inventory** — the concurrency-critical entity. Tracks `totalCount`, `bookedCount`, and `reservedCount` per room/date, plus a cached `price` and denormalized `city` for fast search. Protected by pessimistic locking during booking operations.

**Booking** — driven by a seven-state machine (`RESERVED` → `GUESTS_ADDED` → `PAYMENT_PENDING` → `CONFIRMED` → `CANCELLED_BY_USER` / `CANCELLED_BY_HOTEL_MANAGER`, with an `EXPIRED` timeout branch). Stores the Stripe `paymentSessionId` for webhook correlation.

**Guest** — a reusable profile owned by an `AppUser`, attached to bookings via a `booking_guests` join table so the same guest can be reused across multiple stays.

**Holiday** — a composite-key lookup table `(date, countryCode)` populated from the Calendarific API and cached indefinitely, feeding the holiday pricing multiplier.

**HotelMinPrice** — a denormalized cache of the cheapest available room price per hotel/date, recalculated every 30 minutes, used to keep city-wide search queries fast without joining to `Inventory`.

[Back to top](#table-of-contents)

---

## 7. Business Logic Deep Dive

### 7.1 Booking Flow

**Step 1 — Initialize booking (30-minute reservation window):**

```http
POST /bookings/init
{
  "hotelId": 1,
  "roomId": 5,
  "checkInDate": "2026-03-15",
  "checkOutDate": "2026-03-18",
  "roomsCount": 2
}
```

The service acquires a pessimistic lock on the relevant inventory rows, validates availability (`totalCount - reservedCount - bookedCount >= roomsCount`), increments `reservedCount` atomically, and creates a `Booking` in the `RESERVED` state. A 30-minute expiration timer begins at this point.

```java
int modifiedRows = inventoryRepository.incrementReservedCount(
    hotelId, roomId, checkInDate, checkOutDate, roomsCount
);

int expectedRows = (int) DAYS.between(checkInDate, checkOutDate);
if (modifiedRows != expectedRows) {
    throw new InventoryModifiedException("Inventory changed during booking");
}
```

Row-count validation guards against inventory being closed mid-transaction, catching date-range errors and partial-booking failures before they can corrupt state.

**Step 2 — Add guests (optional):**

```http
POST /bookings/{bookingId}/addGuests
{ "guestIds": [1, 2, 3] }
```

Guest count is validated against `roomsCount × room.capacity`, all guests must belong to the requesting user, and the booking must not have expired. This transitions `RESERVED` → `GUESTS_ADDED`.

**Step 3 — Initiate payment:**

```http
POST /bookings/{bookingId}/payments
```

A Stripe Checkout session is created for the booking amount; the session ID is stored on the booking, and the status moves to `PAYMENT_PENDING`.

**Step 4 — Payment webhook:**

```http
POST /stripe/webhook
Event: checkout.session.completed
```

The Stripe signature is verified, an idempotency check skips already-`CONFIRMED` bookings, the inventory lock is re-acquired, `reservedCount` is decremented while `bookedCount` is incremented atomically, and the booking transitions to `CONFIRMED`.

[Back to Business Logic Deep Dive](#7-business-logic-deep-dive) | [Back to top](#table-of-contents)

---

### 7.2 Dynamic Pricing Engine

**Formula:**

```
finalPrice = basePrice × surgeFactor × urgencyMultiplier × holidayMultiplier × occupancyMultiplier
```

**Implementation — decorator pattern:**

```java
PricingStrategy strategy = new BasePricingStrategy();
strategy = new SurgePricingStrategy(strategy);
strategy = new UrgencyPricingStrategy(strategy);
strategy = new HolidayPricingStrategy(strategy);
strategy = new OccupancyPricingStrategy(strategy);

BigDecimal price = strategy.calculatePrice(inventory);
```

- **Base pricing** — the room's manager-configured rate.
- **Surge pricing** — a manual multiplier set per room/date by the hotel manager (e.g., 2.0x for New Year's Eve), applied only to inventory that is not closed.
- **Urgency pricing** — increases as check-in approaches: +50% within 3 days, +30% within 7 days, +10% within 14 days.
- **Holiday pricing** — a +30% multiplier applied on detected holidays or weekends, sourced from the Calendarific API (all countries supported via ISO 3166-1 alpha-2 codes, with six rotating API keys to stay under rate limits) and cached indefinitely.
- **Occupancy pricing** — increases as availability shrinks: +50% at 90%+ occupancy, +30% at 70%+, +10% at 50%+.

A scheduled job recalculates prices for every active hotel every 30 minutes and refreshes the `HotelMinPrice` cache used for search.

[Back to Business Logic Deep Dive](#7-business-logic-deep-dive) | [Back to top](#table-of-contents)

---

### 7.3 Inventory Management

Hotel managers can close inventory for a date range:

```http
PUT /admin/hotels/{hotelId}/rooms/{roomId}/inventory
{
  "startDate": "2026-04-01",
  "endDate": "2026-04-07",
  "closed": true,
  "surgeFactor": null
}
```

The service locks the affected inventory rows, finds every booking that overlaps the closed range (partial or full overlap), cancels each one, and — for confirmed bookings — routes through the full refund flow. All affected users receive a cancellation email. The entire booking is cancelled rather than split around the closed dates, prioritizing a continuous-stay guest experience and avoiding partial-refund complexity.

[Back to Business Logic Deep Dive](#7-business-logic-deep-dive) | [Back to top](#table-of-contents)

---

### 7.4 Payment and Refunds

**Refund policy matrix:**

| Scenario | Cancelled by | Timing | Refund |
|---|---|---|---|
| Before check-in | User | 7+ days out | 100% |
| Before check-in | User | 3–6 days out | 50% |
| Before check-in | User | <3 days out | 0% |
| Mid-stay or after checkout | User | N/A | 0% |
| Before check-in | Hotel | Any | 100% |
| Mid-stay | Hotel | N/A | Remaining nights |
| After checkout | Hotel | N/A | 0% |

Refunds are processed through Stripe's Refund API. If a Stripe refund call fails (network timeout, insufficient balance, unsettled payment), the failure is logged rather than thrown — the booking is already cancelled and inventory already released, so raising an exception at that point would leave no consistent rollback path. Failed refunds are queued for manual reconciliation via the Stripe dashboard.

[Back to Business Logic Deep Dive](#7-business-logic-deep-dive) | [Back to top](#table-of-contents)

---

## 8. API Documentation

### Authentication

```http
POST /auth/signup
POST /auth/login
POST /auth/refresh
```

Returns a JWT access token in the response body and a refresh token in an HttpOnly cookie.

### Public search

```http
POST /hotels/search
GET  /hotels/{hotelId}/info
```

Search queries the `HotelMinPrice` table directly rather than joining to `Inventory`, keeping city-wide filtering fast.

### User endpoints (authenticated)

```http
POST   /bookings/init
POST   /bookings/{bookingId}/addGuests
DELETE /bookings/{bookingId}/removeGuests
POST   /bookings/{bookingId}/payments
GET    /bookings/{bookingId}/status
GET    /users/my-bookings
DELETE /bookings/{bookingId}
POST   /users/guests
GET    /users/guests
DELETE /users/guests/{guestId}
```

### Admin endpoints (`HOTEL_MANAGER` role)

```http
POST   /admin/hotels
PUT    /admin/hotels/{hotelId}
DELETE /admin/hotels/{hotelId}
POST   /admin/hotels/{hotelId}/rooms
GET    /admin/hotels/{hotelId}/rooms
PUT    /admin/hotels/{hotelId}/rooms/{roomId}
DELETE /admin/hotels/{hotelId}/rooms/{roomId}
GET    /admin/hotels/{hotelId}/rooms/{roomId}/inventory
PUT    /admin/hotels/{hotelId}/rooms/{roomId}/inventory
GET    /admin/hotels/{hotelId}/report
```

### Webhooks

```http
POST /stripe/webhook
```

Handles `checkout.session.completed` (confirms bookings); `checkout.session.expired` handling is planned as a future enhancement.

Full request/response schemas are available via the Swagger UI once the application is running, at `/swagger-ui.html`.

[Back to top](#table-of-contents)

---

## 9. Security and Authentication

**JWT-based stateless authentication:** access and refresh tokens are issued on login. Access tokens are sent as a `Bearer` header; refresh tokens live in an HttpOnly cookie and are exchanged for new access tokens via `/auth/refresh`.

**Password storage:** BCrypt hashing via Spring Security, never plaintext.

**Role-based access control:** the `USER` and `HOTEL_MANAGER` roles gate access to admin endpoints at the controller level via Spring Security configuration.

**Webhook authenticity:** Stripe webhook payloads are verified against the configured signing secret before any state change is applied, preventing spoofed payment confirmations.

[Back to top](#table-of-contents)

---

## 10. Scheduled Jobs

**Expired booking cleanup** — runs every 5 minutes. Finds bookings still in `RESERVED`, `GUESTS_ADDED`, or `PAYMENT_PENDING` status that were created more than 30 minutes ago, releases their reserved inventory, and marks them `EXPIRED`. Batch processing was chosen over per-booking scheduled tasks or a Redis TTL to keep the design simple and dependency-free.

**Dynamic pricing update** — runs every 30 minutes. Recalculates price for every active hotel's inventory using the full pricing strategy chain and refreshes the `HotelMinPrice` cache. Each hotel is processed in its own transaction so one failure does not roll back the rest.

**Startup price initialization** — runs once on `ApplicationReadyEvent` to ensure prices are fresh immediately after a deployment or restart, using the same logic as the scheduled job.

[Back to top](#table-of-contents)

---

## 11. Advanced Features and Design Decisions

**Pessimistic locking** — `@Lock(LockModeType.PESSIMISTIC_WRITE)` on inventory queries ensures that concurrent booking attempts for the same room/date are serialized at the database level, eliminating lost updates without relying on application-level synchronization.

**Row count validation** — every atomic inventory UPDATE checks that the number of affected rows matches the expected number of nights; a mismatch (e.g., because a manager closed inventory mid-transaction) triggers a rollback rather than a silent partial booking.

**State machine validation** — a `Map<BookingStatus, Set<BookingStatus>>` of valid transitions is checked before every status change, preventing invalid transitions such as `CONFIRMED` → `RESERVED`.

**Idempotency protection** — Stripe webhook handling checks whether a booking is already `CONFIRMED` before processing, since Stripe may redeliver the same event after a network retry.

**Exclusive checkout date logic** — nights are computed as `DAYS.between(checkIn, checkOut)` with no `+1`, and inventory queries use `date >= checkIn AND date < checkOut`, matching standard industry booking-platform conventions.

**Transactional boundaries in scheduled jobs** — `@Transactional` is applied per-hotel inside the pricing update loop, not around the whole batch, so a failure for one hotel doesn't roll back successfully updated hotels.

[Back to top](#table-of-contents)

---

## 12. Challenges and Solutions

**Race conditions in concurrent bookings** — solved with pessimistic locking plus atomic UPDATE statements with row-count validation, so two users competing for the same last room can't both succeed.

**30-minute expiration without leaking inventory** — solved with a single batched scheduled job every 5 minutes rather than per-booking timers, a database TTL (not available in PostgreSQL), or an external Redis dependency.

**Hotel closes inventory mid-booking** — resolved by cancelling the entire affected booking with a full refund, rather than splitting it around the closed dates, to preserve a continuous guest experience and avoid complex partial-refund math.

**Stripe refund failures** — logged for manual intervention rather than thrown, since by that point the booking is already cancelled and inventory already released; rolling back would leave an inconsistent state.

**Date range calculations** — nights and inventory-affecting date ranges both use an exclusive checkout date, avoiding the common off-by-one error of adding 1 to the night count.

**Transaction isolation in scheduled jobs** — each hotel's price recalculation runs in its own transaction so that a single hotel's failure doesn't block or roll back updates to every other hotel.

[Back to top](#table-of-contents)

---

## 13. Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.1 |
| Persistence | Spring Data JPA, Hibernate 7.2.0 |
| Database | PostgreSQL 18.1 |
| Security | Spring Security, JJWT 0.12.5, BCrypt |
| Payments | Stripe Java SDK 31.3.0 |
| External APIs | Calendarific (holiday data), RestTemplate |
| Email | Spring Mail, Thymeleaf (HTML templates), Gmail SMTP |
| Documentation | SpringDoc OpenAPI 2.8.3 (Swagger UI) |
| Mapping | ModelMapper 3.2.6 |
| Boilerplate Reduction | Lombok |
| Build Tool | Maven |

[Back to top](#table-of-contents)

---

## 14. Configuration Management

All environment-specific configuration lives in `src/main/resources/application-local.yml` for local development, covering database credentials, JWT secret, Stripe keys, Calendarific API keys, and Gmail SMTP credentials.

For production, the same values are supplied as environment variables instead:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db-host:5432/airbnb_db
export SPRING_DATASOURCE_USERNAME=airbnb_user
export SPRING_DATASOURCE_PASSWORD=prod_password

export JWT_SECRET_KEY=your-production-secret-key-min-32-chars

export STRIPE_SECRET_KEY=sk_live_your_live_key
export STRIPE_WEBHOOK_SECRET_KEY=whsec_your_production_webhook_secret

export SPRING_MAIL_USERNAME=noreply@yourdomain.com
export SPRING_MAIL_PASSWORD=your_smtp_password

export CALENDARIFIC_SECRET_KEY_1=key1
# ... through CALENDARIFIC_SECRET_KEY_6

export FRONTEND_URL=https://yourdomain.com
```

**Security practices:** never commit `application-local.yml`; use `ddl-auto: validate` (not `update`) in production; rotate the JWT secret periodically; use Stripe live keys only in production; terminate TLS at a reverse proxy such as Nginx.

[Back to top](#table-of-contents)

---

## 15. Getting Started

### Prerequisites

- Java 21+
- PostgreSQL 18.1+
- Maven 3.8+
- A Stripe account (test mode is free)
- Calendarific API keys (six free-tier keys recommended for rotation)
- A Gmail account for SMTP

### Clone and configure

```bash
git clone https://github.com/nikhil4457/airbnb-backend.git
cd airbnb-backend
```

Create the database:

```sql
CREATE DATABASE airbnb_db;
CREATE USER airbnb_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE airbnb_db TO airbnb_user;
\c airbnb_db
GRANT ALL ON SCHEMA public TO airbnb_user;
```

Create `src/main/resources/application-local.yml` with your datasource, JWT secret, Stripe keys, Calendarific keys, and mail credentials. Hibernate will auto-create tables on first run via `ddl-auto: update`.

### Build and run

```bash
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=local

# or, from the packaged jar
java -jar target/airbnb-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

The application starts at `http://localhost:8080/api/v1`.

### Verify

```bash
curl http://localhost:8080/api/v1/actuator/health
open http://localhost:8080/api/v1/swagger-ui.html
```

### Test Stripe webhooks locally

```bash
stripe listen --forward-to localhost:8080/api/v1/stripe/webhook
stripe trigger checkout.session.completed
```

[Back to top](#table-of-contents)

---

## 16. Acknowledgments

- **Spring Boot Team** — for the framework and documentation.
- **Stripe** — for a developer-friendly payment API.
- **Calendarific** — for comprehensive holiday data across all countries.
- **PostgreSQL** — for a reliable, feature-rich database.
- **Stack Overflow Community** — for solutions to countless challenges.

[Back to top](#table-of-contents)

---

## 17. Contact

**Developer:** nikhil4457
**GitHub:** [github.com/nikhil4457](https://github.com/nikhil4457)
**LinkedIn:** [linkedin.com/in/nikhil-soni-803aa5293](https://www.linkedin.com/in/nikhil-soni-803aa5293/)

[Back to top](#table-of-contents)

---

## 18. Project Stats and Learning Outcomes

**Project stats:**

| Metric | Value |
|---|---|
| Lines of code | ~5,000+ (Java + SQL) |
| Entities | 10 core entities |
| API endpoints | 30+ RESTful endpoints |
| Database tables | 15+ (including join tables) |
| External integrations | 3 (Stripe, Calendarific, Gmail) |
| Scheduled jobs | 3 (pricing, cleanup, startup) |

**Learning outcomes:** this project demonstrates practical experience with the Spring Boot ecosystem (Data JPA, Security, Mail, Scheduling), relational database design, concurrency control (pessimistic locking, row-count validation), payment integration (Stripe checkout, webhooks, refunds), state-machine-based workflow management, RESTful API design, external API integration with retry/rotation logic, templated email delivery, cron-based scheduled tasks, transaction management, and JWT-based security with role-based access control.

[Back to top](#table-of-contents)

---

*Built by Nikhil Soni*
