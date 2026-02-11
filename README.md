# 🏨 AirBnb Clone Backend

> A production-ready hotel booking platform with real-time inventory management, dynamic pricing, and Stripe payment integration

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18.1-blue.svg)](https://www.postgresql.org/)
[![Stripe](https://img.shields.io/badge/Stripe-Latest-blueviolet.svg)](https://stripe.com/)

---

## 📖 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Tech Stack](#-tech-stack)
- [System Architecture](#-system-architecture)
- [Database Schema](#-database-schema)
- [Business Logic Deep Dive](#-business-logic-deep-dive)
  - [Booking Flow](#1-booking-flow)
  - [Dynamic Pricing](#2-dynamic-pricing-engine)
  - [Inventory Management](#3-inventory-management)
  - [Payment & Refunds](#4-payment--refunds)
- [API Documentation](#-api-documentation)
- [Setup & Installation](#-setup--installation)
- [Environment Variables](#-environment-variables)
- [Scheduled Jobs](#-scheduled-jobs)
- [Advanced Features](#-advanced-features)
- [Challenges & Solutions](#-challenges--solutions)
- [Future Enhancements](#-future-enhancements)

---

## 🎯 Overview

This is a **full-featured hotel booking backend** built with Spring Boot, designed to handle complex scenarios like concurrent bookings, real-time inventory management, dynamic pricing, and Stripe payment processing. The system implements sophisticated business logic including pessimistic locking for race condition prevention, state machine-based booking workflows, and intelligent refund policies.

**Built by:** [nikhil4457](https://github.com/nikhil4457)

---

## ✨ Key Features

### 🔐 **Authentication & Authorization**
- JWT-based authentication with access + refresh tokens
- Role-based access control (USER, HOTEL_MANAGER)
- Secure refresh token stored in HttpOnly cookies
- Spring Security integration with UserDetails

### 🏨 **Hotel & Room Management**
- Multi-room hotel support with flexible room types (manager-defined)
- Room capacity and count management
- Hotel amenities and photos (URLs stored, cloud integration planned)
- Base pricing configuration per room type
- Automated inventory generation (365 days ahead)
- Soft delete with `active` flag

### 📅 **Real-Time Booking System**
- **30-minute reservation window** for completing payment
- Prevents double-booking with **pessimistic locking**
- State machine-based booking workflow (7 states)
- Guest management (add/remove guests with validation)
- Automated cleanup of expired bookings (every 5 minutes)
- Booking history tracking with timestamps

### 💰 **Dynamic Pricing Engine**
Multi-layered pricing strategy:
- **Base Price:** Room's default rate (manager-configured)
- **Surge Pricing:** Manual surge factors set by hotel managers
- **Urgency Pricing:** Prices increase as check-in date approaches
- **Holiday Pricing:** Integration with Calendarific API (supports all countries, 6 API keys for redundancy)
- **Occupancy Pricing:** Prices increase based on room availability
- **HotelMinPrice Optimization:** Cached minimum prices for fast search queries

### 💳 **Stripe Payment Integration**
- Secure checkout sessions with `paymentSessionId` tracking
- Webhook handling for payment confirmation
- Idempotency checks to prevent duplicate processing
- **Intelligent Refund Policies:**
  - User cancels 7+ days before: 100% refund
  - User cancels 3-6 days before: 50% refund
  - User cancels <3 days before: No refund
  - User cancels mid-stay: No refund
  - Hotel cancels: Full refund (or partial if mid-stay)

### 🛠️ **Inventory Management**
- Hotel managers can close inventory for specific date ranges
- Automatic cancellation and refund of affected bookings
- Email notifications sent to affected customers
- Separate inventory tracking for each room/date combination
- Surge factor updates with conditional SQL logic
- Three-tier count system: `totalCount`, `bookedCount`, `reservedCount`

### 📊 **Analytics & Reporting**
- Hotel revenue reports by date range
- Total confirmed bookings count
- Average revenue per booking
- User booking history with status tracking

### 📧 **Email Notifications**
- Booking cancellation emails (HTML templates)
- Thymeleaf template engine for professional emails
- Gmail SMTP integration with configurable sender

---

## 🛠 Tech Stack

### **Backend**
- **Spring Boot 4.0.1** - Core framework
- **Spring Data JPA** - Database abstraction with custom queries
- **Spring Security** - Authentication & authorization
- **Hibernate 7.2.0** - ORM with pessimistic locking
- **PostgreSQL 18.1** - Primary database

### **Payment Processing**
- **Stripe Java SDK 31.3.0** - Payment gateway integration
- **Webhook validation** - Secure payment confirmation

### **External APIs**
- **Calendarific API** - Holiday detection (all countries supported, 6 API keys for rate limit handling)
- **RestTemplate** - HTTP client for external API calls

### **Authentication**
- **JJWT 0.12.5** - JWT token generation/validation
- **BCrypt** - Password hashing via Spring Security

### **Email**
- **Spring Mail** - Email sending
- **Thymeleaf** - HTML email templates

### **Documentation**
- **SpringDoc OpenAPI 2.8.3** - Swagger UI integration

### **Tools**
- **Lombok** - Boilerplate reduction
- **ModelMapper 3.2.6** - DTO mapping

---

## 🏗 System Architecture

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

---

## 🗄 Database Schema

### **Entity Relationship Diagram**

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

---

### **Core Entities**

#### **AppUser** 
`implements UserDetails` (Spring Security integration)

```sql
Table: app_user

Columns:
- id                BIGSERIAL PRIMARY KEY
- email             VARCHAR UNIQUE NOT NULL  -- Used as username
- password          VARCHAR NOT NULL         -- BCrypt hashed
- name              VARCHAR
- date_of_birth     DATE
- gender            VARCHAR                  -- MALE, FEMALE, OTHER

Separate Table (ElementCollection):
- user_roles
  ├─ app_user_id (FK)
  └─ roles (VARCHAR)  -- USER, HOTEL_MANAGER
```

**Key Features:**
- Implements Spring Security's `UserDetails` interface
- `getUsername()` returns email for authentication
- `getAuthorities()` converts roles to `ROLE_` prefixed SimpleGrantedAuthority
- EAGER fetch on roles for authentication
- Custom `equals()` and `hashCode()` based on `id`

**Relationships:**
- Owns multiple Hotels (as `owner`)
- Creates multiple Bookings (as `user`)
- Manages multiple Guest profiles

**Enums:**
```java
Gender: MALE, FEMALE, OTHER
Role: USER, HOTEL_MANAGER
```

---

#### **Hotel**

```sql
Table: hotel

Columns:
- id                BIGSERIAL PRIMARY KEY
- name              VARCHAR NOT NULL
- city              VARCHAR              -- Indexed for search
- active            BOOLEAN NOT NULL     -- Soft delete flag
- owner_id          BIGINT NOT NULL (FK → app_user)
- address           VARCHAR              -- From embedded HotelContactInfo
- phone_number      VARCHAR
- email             VARCHAR
- location          VARCHAR              -- Latitude/longitude string (PostGIS planned)
- created_at        TIMESTAMP            -- Auto-generated
- updated_at        TIMESTAMP            -- Auto-updated

Separate Tables (ElementCollection):
- hotel_images
  ├─ hotel_id (FK)
  └─ image_url VARCHAR NOT NULL       -- Photo URLs (Cloudinary integration planned)

- hotel_amenities
  ├─ hotel_id (FK)
  └─ amenity VARCHAR                   -- e.g., "WiFi", "Pool", "Gym", "Parking"
```

**Embedded Object:**
```java
@Embeddable HotelContactInfo {
  - address
  - phoneNumber
  - email
  - location  // String format: "lat,lng" or address
}
```

**Relationships:**
- `owner`: ManyToOne → AppUser (NOT NULL)
- `rooms`: OneToMany → Room (mapped by "hotel")

**Business Rules:**
- `active = false` prevents new bookings (soft delete)
- Hotel photos stored as URLs (cloud storage integration planned)
- Amenities are flexible strings defined by hotel manager

---

#### **Room**

```sql
Table: room

Columns:
- id                BIGSERIAL PRIMARY KEY
- hotel_id          BIGINT NOT NULL (FK → hotel)
- type              VARCHAR NOT NULL     -- Manager-defined (e.g., "Deluxe Suite", "Ocean View")
- base_price        DECIMAL(10,2) NOT NULL
- total_count       INTEGER NOT NULL     -- Total rooms of this type
- capacity          INTEGER NOT NULL     -- Max guests per room
- created_at        TIMESTAMP
- updated_at        TIMESTAMP

Separate Tables (ElementCollection):
- room_images
  ├─ room_id (FK)
  └─ image_url VARCHAR NOT NULL

- room_amenities
  ├─ room_id (FK)
  └─ amenity VARCHAR                   -- e.g., "King Bed", "Balcony", "Mini Bar"
```

**Key Features:**
- Room `type` is a free-text field (not enum) for flexibility
- Hotel managers define their own room categories
- Each room type has independent inventory tracking
- Photos and amenities stored as string collections

**Relationships:**
- `hotel`: ManyToOne → Hotel (LAZY, NOT NULL)
- Inventory created for 365 days ahead on room creation

**Validation:**
- `capacity` used to validate guest count during booking
- `totalCount` must match sum of inventory counts

---

#### **Inventory**

```sql
Table: inventory

Columns:
- id                BIGSERIAL PRIMARY KEY
- hotel_id          BIGINT NOT NULL (FK → hotel)
- room_id           BIGINT NOT NULL (FK → room)
- date              DATE NOT NULL
- total_count       INTEGER NOT NULL     -- Total available rooms
- booked_count      INTEGER DEFAULT 0    -- Confirmed bookings
- reserved_count    INTEGER DEFAULT 0    -- 30-min temp holds
- surge_factor      DECIMAL(5,2) NOT NULL
- price             DECIMAL(10,2) NOT NULL  -- basePrice × surgeFactor × multipliers
- city              VARCHAR NOT NULL     -- Denormalized for fast search
- closed            BOOLEAN NOT NULL     -- Hotel manager can close dates
- created_at        TIMESTAMP
- updated_at        TIMESTAMP

Constraints:
- UNIQUE (hotel_id, room_id, date)
```

**Three-Tier Count System:**
```
totalCount = 10 rooms
bookedCount = 3  (confirmed, paid bookings)
reservedCount = 2  (30-min holds, awaiting payment)
Available = totalCount - bookedCount - reservedCount = 5
```

**Denormalization:**
- `city` copied from Hotel for fast search queries (avoids JOIN)
- `price` cached after dynamic pricing calculation

**Pessimistic Locking:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<Inventory> findAndLockAvailableInventory(...);
```
Prevents race conditions during concurrent bookings.

**Business Rules:**
- When `closed = true`, no new bookings allowed
- Existing bookings cancelled when inventory closed
- Price updated every 30 minutes via scheduled job

---

#### **Booking**

```sql
Table: booking

Columns:
- id                    BIGSERIAL PRIMARY KEY
- hotel_id              BIGINT NOT NULL (FK → hotel)
- room_id               BIGINT NOT NULL (FK → room)
- user_id               BIGINT NOT NULL (FK → app_user)
- rooms_count           INTEGER NOT NULL
- check_in_date         DATE NOT NULL
- check_out_date        DATE NOT NULL
- amount                DECIMAL(10,2) NOT NULL
- booking_status        VARCHAR NOT NULL     -- See state machine below
- payment_session_id    VARCHAR UNIQUE       -- Stripe checkout session
- created_at            TIMESTAMP            -- For 30-min expiration check
- updated_at            TIMESTAMP

Join Table:
- booking_guests
  ├─ booking_id (FK → booking)
  └─ guest_id (FK → guest)
```

**State Machine (BookingStatus enum):**
```
RESERVED (initial)
    ↓ 30-min timer starts
    ↓ User adds guests (optional)
GUESTS_ADDED
    ↓ User initiates payment
PAYMENT_PENDING
    ↓ Stripe webhook confirms
CONFIRMED ✅
    ↓ User/Hotel cancels
CANCELLED_BY_USER / CANCELLED_BY_HOTEL_MANAGER
    
Timeouts:
RESERVED/GUESTS_ADDED/PAYMENT_PENDING → EXPIRED (after 30 min)
```

**Valid State Transitions:**
```java
RESERVED → GUESTS_ADDED → PAYMENT_PENDING → CONFIRMED
RESERVED → PAYMENT_PENDING → CONFIRMED
RESERVED/GUESTS_ADDED/PAYMENT_PENDING → EXPIRED
CONFIRMED → CANCELLED_BY_USER
CONFIRMED → CANCELLED_BY_HOTEL_MANAGER
```

**Relationships:**
- `user`: ManyToOne → AppUser (LAZY, NOT NULL)
- `hotel`: ManyToOne → Hotel (LAZY, NOT NULL)
- `room`: ManyToOne → Room (LAZY, NOT NULL)
- `guests`: ManyToMany → Guest (join table: `booking_guests`)

**Business Rules:**
- Guest count ≤ `roomsCount × room.capacity`
- Check-out date exclusive: WHERE `date >= checkInDate AND date < checkOutDate`
- Nights = `DAYS.between(checkInDate, checkOutDate)` (no +1 needed!)

---

#### **Guest**

```sql
Table: guest

Columns:
- id                BIGSERIAL PRIMARY KEY
- user_id           BIGINT (FK → app_user)  -- Owner of this profile
- name              VARCHAR NOT NULL
- gender            VARCHAR                  -- MALE, FEMALE, OTHER
- age               INTEGER
```

**Key Features:**
- Reusable guest profiles (user creates once, reuses for multiple bookings)
- Stored under user's account for convenience
- Many-to-Many relationship with Booking via `booking_guests`

**Relationships:**
- `user`: ManyToOne → AppUser (the owner/creator of this guest profile)
- Referenced by multiple bookings via join table

**Validation:**
- User can only add their own guests to bookings
- Guest count validated against room capacity

---

#### **Holiday**

```sql
Table: holidays

Composite Primary Key:
- date              DATE NOT NULL
- country_code      VARCHAR NOT NULL  -- ISO 3166-1 alpha-2 (e.g., "US", "IN", "GB")

No additional columns (just PK)
```

**Composite Key Implementation:**
```java
@Embeddable
class HolidayId implements Serializable {
  - LocalDate date
  - CountryCode countryCode  // Enum with all countries
}
```

**Supported Countries:**
- **All countries** via CountryCode enum (ISO 3166-1 alpha-2 codes)
- Examples: IN (India), US (United States), GB (United Kingdom), etc.

**Data Source:**
- Populated from Calendarific API
- 6 API keys rotated to avoid rate limits
- Cached indefinitely (holidays don't change)
- Weekend detection (Saturday/Sunday) handled separately

**Usage:**
- Holiday pricing multiplier (1.3x)
- Weekend pricing (separate logic)
- Country-specific holiday detection for hotels in different cities

---

#### **HotelMinPrice**

```sql
Table: hotel_min_prices

Columns:
- id                BIGSERIAL PRIMARY KEY
- hotel_id          BIGINT NOT NULL (FK → hotel)
- date              DATE NOT NULL
- min_price         DECIMAL(10,2) NOT NULL  -- Cheapest room for this hotel on this date
- created_at        TIMESTAMP
- updated_at        TIMESTAMP

Constraints:
- UNIQUE (hotel_id, date)
```

**Purpose:**
- **Search Optimization:** Enables fast "hotels in city with price" queries
- Avoids expensive JOIN to Inventory table during search
- Updated every 30 minutes via scheduled job

**How It Works:**
```sql
-- Instead of:
SELECT h.* FROM hotel h 
JOIN inventory i ON h.id = i.hotel_id 
WHERE i.city = 'Mumbai' 
GROUP BY h.id 
HAVING MIN(i.price) <= 5000;

-- We do:
SELECT h.* FROM hotel h
JOIN hotel_min_prices hmp ON h.id = hmp.hotel_id
WHERE h.city = 'Mumbai' AND hmp.date = '2026-03-15' AND hmp.min_price <= 5000;
```

**Maintenance:**
```java
@Scheduled(cron = "0 */30 * * * *")
// Recalculates min price for each hotel/date combination
```

---

## 💡 Business Logic Deep Dive

### **1. Booking Flow**

#### **Step 1: Initialize Booking (30-min reservation)**
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

**Process:**
1. **Pessimistic lock** on inventory rows for date range
2. Validate availability: `(totalCount - reservedCount - bookedCount) >= roomsCount`
3. Calculate total price from inventory (dynamic pricing already applied)
4. **Increment `reservedCount`** in inventory (atomic operation)
5. Create booking with status `RESERVED`
6. Store `createdAt` timestamp
7. **30-minute expiration timer starts**

**Key Implementation Detail:**
```java
// Custom query with row count validation
int modifiedRows = inventoryRepository.incrementReservedCount(
    hotelId, roomId, checkInDate, checkOutDate, roomsCount
);

// Ensure UPDATE affected expected number of rows
int expectedRows = (int) DAYS.between(checkInDate, checkOutDate);
if (modifiedRows != expectedRows) {
    throw new InventoryModifiedException("Inventory changed during booking");
}
```

**Why Row Count Validation?**
- Detects if inventory was closed mid-transaction
- Catches date range calculation errors
- Prevents partial booking failures

---

#### **Step 2: Add Guests (Optional)**
```http
POST /bookings/{bookingId}/addGuests
{
  "guestIds": [1, 2, 3]
}
```

**Validation:**
- Booking must be in `RESERVED` or `GUESTS_ADDED` status
- Guest count ≤ `roomsCount × room.capacity`
- All guests must belong to current user (security check)
- Booking not expired (created within last 30 minutes)

**State Transition:**
- `RESERVED` → `GUESTS_ADDED`
- If already `GUESTS_ADDED`, updates guest list

**Remove Guests:**
```http
DELETE /bookings/{bookingId}/removeGuests
{
  "guestIds": [2]
}
```

---

#### **Step 3: Initiate Payment**
```http
POST /bookings/{bookingId}/payments
```

**Creates Stripe Checkout Session:**
```java
SessionCreateParams params = SessionCreateParams.builder()
    .setMode(SessionCreateParams.Mode.PAYMENT)  // One-time charge
    .setSuccessUrl(frontendUrl + "/booking/success?session_id={CHECKOUT_SESSION_ID}")
    .setCancelUrl(frontendUrl + "/booking/cancel")
    .addLineItem(
        SessionCreateParams.LineItem.builder()
            .setPriceData(
                SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency("usd")
                    .setUnitAmount(amount.multiply(new BigDecimal("100")).longValue())
                    .setProductData(
                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName(hotel.getName() + " - " + room.getType())
                            .build()
                    )
                    .build()
            )
            .setQuantity(1L)
            .build()
    )
    .build();

Session session = Session.create(params);
```

**Updates Booking:**
- Stores `paymentSessionId` from Stripe
- Sets status to `PAYMENT_PENDING`
- User redirected to Stripe checkout page

---

#### **Step 4: Payment Webhook (Stripe → Server)**
```http
POST /stripe/webhook
Event: checkout.session.completed
```

**Process:**
1. **Validate Stripe signature** (prevents spoofing)
   ```java
   Event event = Webhook.constructEvent(
       payload, sigHeader, webhookSecret
   );
   ```

2. **Idempotency check:**
   ```java
   if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
       log.info("Already processed, skipping duplicate webhook");
       return;
   }
   ```

3. **Pessimistic lock on inventory** (same date range)

4. **Atomic inventory update:**
   ```java
   int rows = inventoryRepository.confirmBooking(
       hotelId, roomId, checkInDate, checkOutDate, roomsCount
   );
   // Decrements reservedCount, increments bookedCount
   validateCountOfRows(checkInDate, checkOutDate, rows);
   ```

5. **Update booking status:**
   - `PAYMENT_PENDING` → `CONFIRMED`
   - Validate state transition

**Why Idempotency Matters:**
- Stripe may send duplicate webhooks (network retries)
- Prevents double-charging inventory
- Prevents duplicate email notifications

---

### **2. Dynamic Pricing Engine**

**Pricing Formula:**
```
finalPrice = basePrice × surgeFactor × urgencyMultiplier × holidayMultiplier × occupancyMultiplier
```

#### **Implementation: Decorator Pattern**
```java
// Chain of responsibility for pricing strategies
PricingStrategy strategy = new BasePricingStrategy();
strategy = new SurgePricingStrategy(strategy);
strategy = new UrgencyPricingStrategy(strategy);
strategy = new HolidayPricingStrategy(strategy);
strategy = new OccupancyPricingStrategy(strategy);

BigDecimal price = strategy.calculatePrice(inventory);
```

---

#### **A. Base Pricing**
```java
BigDecimal basePrice = room.getBasePrice();  // Manager-configured
```

---

#### **B. Surge Pricing**
```java
BigDecimal surgeFactor = inventory.getSurgeFactor();  // Manager-configured (default 1.0)
BigDecimal price = basePrice.multiply(surgeFactor);
```

**Hotel Manager Control:**
- Can set surge factor per room/date via API
- Example: Set surgeFactor = 2.0 for New Year's Eve
- Conditional update (only if inventory not closed):
  ```sql
  UPDATE inventory 
  SET surge_factor = :surgeFactor 
  WHERE room_id = :roomId 
    AND date >= :startDate 
    AND date < :endDate 
    AND closed = false
  ```

---

#### **C. Urgency Pricing**
```java
long daysUntilCheckIn = DAYS.between(LocalDate.now(), inventory.getDate());

if (daysUntilCheckIn <= 3) {
    multiplier = 1.5;  // 50% increase
} else if (daysUntilCheckIn <= 7) {
    multiplier = 1.3;  // 30% increase
} else if (daysUntilCheckIn <= 14) {
    multiplier = 1.1;  // 10% increase
} else {
    multiplier = 1.0;  // No increase
}

price = price.multiply(multiplier);
```

**Logic:**
- Incentivizes early bookings
- Last-minute bookings pay premium
- Recalculated every 30 minutes

---

#### **D. Holiday Pricing**
```java
boolean isHoliday = holidayRepository.existsByIdDateAndIdCountryCode(
    inventory.getDate(), 
    getCountryCodeFromCity(inventory.getCity())
);

boolean isWeekend = inventory.getDate().getDayOfWeek() == SATURDAY 
                 || inventory.getDate().getDayOfWeek() == SUNDAY;

if (isHoliday || isWeekend) {
    multiplier = 1.3;  // 30% increase
} else {
    multiplier = 1.0;
}

price = price.multiply(multiplier);
```

**Holiday Data Source:**
- **Calendarific API** with 6 API keys (rotating to avoid rate limits)
- Supports **all countries** via ISO 3166-1 alpha-2 codes
- Cached indefinitely in `holidays` table
- Country determined from hotel's city

**API Key Rotation:**
```java
private int currentKeyIndex = 0;
private final List<String> apiKeys = List.of(key1, key2, key3, key4, key5, key6);

private String getNextApiKey() {
    String key = apiKeys.get(currentKeyIndex);
    currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
    return key;
}
```

---

#### **E. Occupancy Pricing**
```java
int availableRooms = inventory.getTotalCount() 
                   - inventory.getBookedCount() 
                   - inventory.getReservedCount();

double occupancyRate = 1.0 - (availableRooms / (double) inventory.getTotalCount());

if (occupancyRate >= 0.9) {
    multiplier = 1.5;  // 50% increase (almost sold out)
} else if (occupancyRate >= 0.7) {
    multiplier = 1.3;  // 30% increase (high demand)
} else if (occupancyRate >= 0.5) {
    multiplier = 1.1;  // 10% increase (moderate demand)
} else {
    multiplier = 1.0;  // No increase (low demand)
}

price = price.multiply(multiplier);
```

**Logic:**
- Prices increase as availability decreases
- Encourages early bookings
- Maximizes revenue during peak demand

---

#### **Scheduled Pricing Updates**
```java
@Scheduled(cron = "0 */30 * * * *")  // Every 30 minutes
public void updateAllHotelPrices() {
    List<Hotel> hotels = hotelRepository.findAll();
    
    for (Hotel hotel : hotels) {
        try {
            updatePricesForHotel(hotel);  // Each in separate transaction
        } catch (Exception e) {
            log.error("Failed to update prices for hotel {}", hotel.getId(), e);
            // Continue with other hotels
        }
    }
}
```

**Also Updates HotelMinPrice:**
```java
for (LocalDate date : dateRange) {
    BigDecimal minPrice = inventoryRepository
        .findMinPriceForHotelOnDate(hotelId, date);
    
    hotelMinPriceRepository.upsert(hotelId, date, minPrice);
}
```

---

### **3. Inventory Management**

#### **Close Inventory (Hotel Manager)**
```http
PUT /admin/hotels/{hotelId}/rooms/{roomId}/inventory
{
  "startDate": "2026-04-01",
  "endDate": "2026-04-07",
  "closed": true,
  "surgeFactor": null
}
```

**Process:**

**1. Lock inventory for date range:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<Inventory> findByRoomIdAndDateBetween(...);
```

**2. Find all intersecting bookings:**
```sql
SELECT * FROM booking 
WHERE room_id = :roomId
  AND booking_status IN ('RESERVED', 'GUESTS_ADDED', 'PAYMENT_PENDING', 'CONFIRMED')
  AND NOT (check_out_date <= :startDate OR check_in_date >= :endDate)
  -- This captures ANY overlap, even partial
```

**Overlap Examples:**
```
Closed Period: Apr 1-7

Overlap Cases:
1. Booking: Mar 28 - Apr 3   ✅ OVERLAPS (ends during closure)
2. Booking: Apr 5 - Apr 10   ✅ OVERLAPS (starts during closure)
3. Booking: Mar 25 - Apr 10  ✅ OVERLAPS (spans entire closure)
4. Booking: Apr 2 - Apr 5    ✅ OVERLAPS (fully contained)
5. Booking: Mar 20 - Mar 28  ❌ No overlap (ends before)
6. Booking: Apr 8 - Apr 15   ❌ No overlap (starts after)
```

**3. Cancel bookings by status:**

**For RESERVED/GUESTS_ADDED/PAYMENT_PENDING:**
```java
// Release reserved inventory
inventoryRepository.decrementReservedCount(
    hotelId, roomId, checkInDate, checkOutDate, roomsCount
);

// Mark as cancelled
booking.setBookingStatus(BookingStatus.CANCELLED_BY_HOTEL_MANAGER);

// Send email notification
emailService.sendCancellationEmail(booking, "Hotel closed inventory");
```

**For CONFIRMED:**
```java
// Call full cancellation flow (handles refunds)
cancelBooking(bookingId, false);  // false = cancelled by hotel

// This processes refund via Stripe
// Decrements bookedCount
// Sends email
```

**4. Set inventory to closed:**
```java
for (Inventory inv : inventories) {
    inv.setClosed(true);
}
inventoryRepository.saveAll(inventories);
```

---

#### **Why Cancel Entire Booking (Not Split)?**

**Scenario:**
```
User booked: Mar 28 - Apr 10 (13 nights)
Hotel closes: Apr 1 - Apr 7

Option A (Current): Cancel entire booking, full refund
Option B (Rejected): Split into Mar 28-31 + Apr 8-10, partial refund
```

**Reasons for Option A:**
1. **User Experience:** Guest expects continuous stay, not interrupted
2. **Simplicity:** No complex partial refund calculations
3. **Fairness:** Hotel's closure, hotel's responsibility
4. **Technical:** No need to create/modify multiple bookings

---

#### **Refund Policy for Hotel Closures:**

| Timing | Refund Amount |
|--------|---------------|
| Before check-in | 100% full refund |
| During stay (partial overlap) | Refund for remaining nights only |
| After checkout | No refund (stay completed) |

---

### **4. Payment & Refunds**

#### **Refund Policy Matrix**

| Scenario | Cancelled By | Days Before Check-in | Refund |
|----------|--------------|---------------------|--------|
| Before check-in | User | 7+ days | 100% |
| Before check-in | User | 3-6 days | 50% |
| Before check-in | User | <3 days | 0% |
| During stay | User | N/A | 0% |
| After checkout | User | N/A | 0% |
| Before check-in | Hotel | Any | 100% |
| During stay | Hotel | N/A | Remaining nights |
| After checkout | Hotel | N/A | 0% |

---

#### **Cancellation Implementation**

```java
public void cancelBooking(Long bookingId, boolean cancelledByUser) {
    Booking booking = findBookingOrThrow(bookingId);
    
    // Validate status
    if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
        throw new InvalidStateException("Only confirmed bookings can be cancelled");
    }
    
    // Calculate refund
    BigDecimal refundAmount = calculateRefund(booking, cancelledByUser);
    
    // Release inventory
    inventoryRepository.decrementBookedCount(
        booking.getHotel().getId(),
        booking.getRoom().getId(),
        booking.getCheckInDate(),
        booking.getCheckOutDate(),
        booking.getRoomsCount()
    );
    
    // Process Stripe refund (if applicable)
    if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
        processStripeRefund(booking.getPaymentSessionId(), refundAmount);
    }
    
    // Update status
    booking.setBookingStatus(
        cancelledByUser 
            ? BookingStatus.CANCELLED_BY_USER 
            : BookingStatus.CANCELLED_BY_HOTEL_MANAGER
    );
    
    // Send email
    emailService.sendCancellationEmail(booking, refundAmount);
}
```

---

#### **Refund Calculation Logic**

```java
private BigDecimal calculateRefund(Booking booking, boolean cancelledByUser) {
    LocalDate today = LocalDate.now();
    LocalDate checkIn = booking.getCheckInDate();
    LocalDate checkOut = booking.getCheckOutDate();
    
    // After checkout - no refund
    if (today.isAfter(checkOut) || today.isEqual(checkOut)) {
        return BigDecimal.ZERO;
    }
    
    // Hotel cancels - always full or partial
    if (!cancelledByUser) {
        if (today.isBefore(checkIn)) {
            return booking.getAmount();  // Full refund
        } else {
            // Mid-stay: refund remaining nights
            return calculateMidStayRefund(booking, today);
        }
    }
    
    // User cancels - apply policy
    if (today.isBefore(checkIn)) {
        long daysUntilCheckIn = DAYS.between(today, checkIn);
        
        if (daysUntilCheckIn >= 7) {
            return booking.getAmount();  // 100%
        } else if (daysUntilCheckIn >= 3) {
            return booking.getAmount().multiply(new BigDecimal("0.5"));  // 50%
        } else {
            return BigDecimal.ZERO;  // No refund
        }
    } else {
        // Mid-stay user cancellation - no refund
        return BigDecimal.ZERO;
    }
}
```

---

#### **Mid-Stay Refund (Hotel Cancellation)**

```java
private BigDecimal calculateMidStayRefund(Booking booking, LocalDate today) {
    // Nights already consumed (including today)
    long nightsCharged = DAYS.between(booking.getCheckInDate(), today) + 1;
    
    // Total nights in booking
    long totalNights = DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
    
    // Remaining nights to refund
    long nightsToRefund = totalNights - nightsCharged;
    
    if (nightsToRefund <= 0) {
        return BigDecimal.ZERO;
    }
    
    // Calculate per-night rate
    BigDecimal perNightRate = booking.getAmount()
        .divide(new BigDecimal(totalNights), 2, RoundingMode.HALF_UP);
    
    // Refund for remaining nights
    return perNightRate.multiply(new BigDecimal(nightsToRefund));
}
```

**Example:**
```
Booking: Apr 1-10 (9 nights), $900 total
Today: Apr 5

Nights charged: 4 (Apr 1,2,3,4) + 1 (Apr 5, today) = 5 nights
Remaining nights: 9 - 5 = 4 nights
Per night rate: $900 / 9 = $100
Refund: $100 × 4 = $400
```

**Why +1 for "today"?**
- Prevents abuse (guest can't check in and immediately cancel)
- Gives hotel time to clean and relist room
- Standard industry practice

---

#### **Stripe Refund Processing**

```java
private void processStripeRefund(String paymentSessionId, BigDecimal amount) {
    try {
        // Retrieve payment intent from session
        Session session = Session.retrieve(paymentSessionId);
        String paymentIntentId = session.getPaymentIntent();
        
        // Create refund
        RefundCreateParams params = RefundCreateParams.builder()
            .setPaymentIntent(paymentIntentId)
            .setAmount(amount.multiply(new BigDecimal("100")).longValue())  // Cents
            .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
            .build();
        
        Refund refund = Refund.create(params);
        
        log.info("Refund processed: {} for amount {}", refund.getId(), amount);
        
    } catch (StripeException e) {
        log.error("Stripe refund failed for session {}", paymentSessionId, e);
        // Don't throw - booking already cancelled, log for manual intervention
    }
}
```

**Error Handling:**
- Network timeouts → Log and continue (retry manually)
- Insufficient funds → Log and continue (contact Stripe support)
- Payment not settled → Log and continue (retry after settlement)

**Why Not Throw Exception?**
- Booking is already cancelled (user's primary concern)
- Inventory is already released (hotel's primary concern)
- Refund can be processed manually via Stripe dashboard

---

## 📡 API Documentation

### **Public Endpoints**

#### **Authentication**
```http
POST /auth/signup
Body: {
  "email": "user@example.com",
  "password": "securePass123",
  "name": "John Doe",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE"
}
Response: {
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."  // Also in HttpOnly cookie
}

POST /auth/login
Body: {
  "email": "user@example.com",
  "password": "securePass123"
}
Response: {
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}

POST /auth/refresh
Headers: Cookie: refreshToken=...
Response: {
  "accessToken": "eyJhbGc..."
}
```

---

#### **Hotel Search**
```http
POST /hotels/search
Body: {
  "city": "Mumbai",
  "startDate": "2026-03-15",
  "endDate": "2026-03-18",
  "roomsCount": 2,
  "page": 0,
  "size": 20
}
Response: {
  "content": [
    {
      "hotelId": 1,
      "name": "Taj Hotel",
      "city": "Mumbai",
      "minPrice": 5000.00,
      "photos": ["url1", "url2"],
      "amenities": ["Pool", "WiFi"]
    }
  ],
  "totalElements": 15,
  "totalPages": 1
}
```

**Query Optimization:**
- Uses `HotelMinPrice` table for fast filtering
- Avoids JOIN to `Inventory` during search
- Filters by city (indexed) and price range

---

#### **Hotel Details**
```http
GET /hotels/{hotelId}/info
Response: {
  "id": 1,
  "name": "Taj Hotel",
  "city": "Mumbai",
  "contactInfo": {
    "address": "Apollo Bunder, Mumbai",
    "phoneNumber": "+91-22-6665-3366",
    "email": "reservations@tajhotels.com",
    "location": "18.9220,72.8332"
  },
  "photos": ["url1", "url2"],
  "amenities": ["Pool", "WiFi", "Gym"],
  "rooms": [
    {
      "id": 5,
      "type": "Deluxe Suite",
      "basePrice": 8000.00,
      "capacity": 3,
      "totalCount": 10,
      "imageUrls": ["room_url1"],
      "amenities": ["King Bed", "Balcony"]
    }
  ]
}
```

---

### **User Endpoints (Authenticated)**

#### **Bookings**

```http
POST /bookings/init
Headers: Authorization: Bearer {accessToken}
Body: {
  "hotelId": 1,
  "roomId": 5,
  "checkInDate": "2026-03-15",
  "checkOutDate": "2026-03-18",
  "roomsCount": 2
}
Response: {
  "bookingId": 123,
  "amount": 48000.00,
  "bookingStatus": "RESERVED",
  "expiresAt": "2026-02-10T15:30:00Z"
}
```

```http
POST /bookings/{bookingId}/addGuests
Body: {
  "guestIds": [1, 2, 3]
}
Response: {
  "bookingId": 123,
  "bookingStatus": "GUESTS_ADDED",
  "guests": [
    {"id": 1, "name": "Alice Doe", "age": 28},
    {"id": 2, "name": "Bob Doe", "age": 30}
  ]
}
```

```http
DELETE /bookings/{bookingId}/removeGuests
Body: {
  "guestIds": [2]
}
```

```http
POST /bookings/{bookingId}/payments
Response: {
  "checkoutUrl": "https://checkout.stripe.com/pay/cs_test_...",
  "sessionId": "cs_test_a1b2c3...",
  "bookingStatus": "PAYMENT_PENDING"
}
```

```http
GET /bookings/{bookingId}/status
Response: {
  "bookingId": 123,
  "bookingStatus": "CONFIRMED",
  "amount": 48000.00,
  "hotel": "Taj Hotel",
  "room": "Deluxe Suite",
  "checkInDate": "2026-03-15",
  "checkOutDate": "2026-03-18"
}
```

```http
GET /users/my-bookings
Query Params: ?page=0&size=10
Response: {
  "content": [
    {
      "bookingId": 123,
      "hotelName": "Taj Hotel",
      "roomType": "Deluxe Suite",
      "checkInDate": "2026-03-15",
      "checkOutDate": "2026-03-18",
      "amount": 48000.00,
      "bookingStatus": "CONFIRMED",
      "createdAt": "2026-02-10T14:00:00Z"
    }
  ],
  "totalElements": 5
}
```

```http
DELETE /bookings/{bookingId}
Response: {
  "message": "Booking cancelled successfully",
  "refundAmount": 48000.00,
  "bookingStatus": "CANCELLED_BY_USER"
}
```

---

#### **Guests**

```http
POST /users/guests
Body: {
  "name": "Alice Doe",
  "gender": "FEMALE",
  "age": 28
}
Response: {
  "id": 1,
  "name": "Alice Doe",
  "gender": "FEMALE",
  "age": 28
}
```

```http
GET /users/guests
Response: [
  {"id": 1, "name": "Alice Doe", "gender": "FEMALE", "age": 28},
  {"id": 2, "name": "Bob Doe", "gender": "MALE", "age": 30}
]
```

```http
DELETE /users/guests/{guestId}
Response: {
  "message": "Guest deleted successfully"
}
```

---

### **Admin Endpoints (HOTEL_MANAGER role)**

#### **Hotels**

```http
POST /admin/hotels
Headers: Authorization: Bearer {accessToken}
Body: {
  "name": "Grand Hyatt",
  "city": "Mumbai",
  "contactInfo": {
    "address": "Santacruz East, Mumbai",
    "phoneNumber": "+91-22-6676-1234",
    "email": "info@grandhyattmumbai.com",
    "location": "19.0896,72.8656"
  },
  "photos": ["url1", "url2"],
  "amenities": ["Pool", "Spa", "WiFi"]
}
Response: {
  "id": 2,
  "name": "Grand Hyatt",
  "active": true,
  "owner": {
    "id": 5,
    "email": "manager@example.com"
  }
}
```

```http
PUT /admin/hotels/{hotelId}
Body: {
  "name": "Grand Hyatt Mumbai",
  "amenities": ["Pool", "Spa", "WiFi", "Gym"]
}
```

```http
DELETE /admin/hotels/{hotelId}
Response: {
  "message": "Hotel marked as inactive"
}
```

---

#### **Rooms**

```http
POST /admin/hotels/{hotelId}/rooms
Body: {
  "type": "Ocean View Suite",
  "basePrice": 12000.00,
  "capacity": 4,
  "totalCount": 5,
  "imageUrls": ["room1.jpg", "room2.jpg"],
  "amenities": ["King Bed", "Balcony", "Mini Bar"]
}
Response: {
  "id": 10,
  "type": "Ocean View Suite",
  "basePrice": 12000.00,
  "capacity": 4,
  "totalCount": 5
}
```

```http
GET /admin/hotels/{hotelId}/rooms
Response: [
  {
    "id": 5,
    "type": "Deluxe Suite",
    "basePrice": 8000.00,
    "capacity": 3,
    "totalCount": 10
  }
]
```

```http
PUT /admin/hotels/{hotelId}/rooms/{roomId}
Body: {
  "basePrice": 9000.00,
  "amenities": ["King Bed", "Balcony", "Mini Bar", "Jacuzzi"]
}
```

```http
DELETE /admin/hotels/{hotelId}/rooms/{roomId}
Response: {
  "message": "Room deleted successfully"
}
```

---

#### **Inventory Management**

```http
GET /admin/hotels/{hotelId}/rooms/{roomId}/inventory
Query Params: ?startDate=2026-04-01&endDate=2026-04-07
Response: [
  {
    "date": "2026-04-01",
    "totalCount": 10,
    "bookedCount": 5,
    "reservedCount": 2,
    "availableCount": 3,
    "price": 10400.00,
    "surgeFactor": 1.3,
    "closed": false
  }
]
```

```http
PUT /admin/hotels/{hotelId}/rooms/{roomId}/inventory
Body: {
  "startDate": "2026-04-01",
  "endDate": "2026-04-07",
  "closed": true,
  "surgeFactor": null
}
Response: {
  "message": "Inventory closed successfully",
  "affectedBookings": 3,
  "refundedAmount": 144000.00
}
```

```http
PUT /admin/hotels/{hotelId}/rooms/{roomId}/inventory
Body: {
  "startDate": "2026-12-31",
  "endDate": "2026-01-02",
  "closed": false,
  "surgeFactor": 2.5
}
Response: {
  "message": "Surge factor updated successfully",
  "affectedDates": 2
}
```

---

#### **Reports**

```http
GET /admin/hotels/{hotelId}/report
Query Params: ?startDate=2026-01-01&endDate=2026-01-31
Response: {
  "totalRevenue": 450000.00,
  "totalBookings": 25,
  "averageRevenuePerBooking": 18000.00,
  "bookingsByStatus": {
    "CONFIRMED": 20,
    "CANCELLED_BY_USER": 3,
    "CANCELLED_BY_HOTEL_MANAGER": 2
  },
  "bookingsByRoomType": {
    "Deluxe Suite": 15,
    "Ocean View Suite": 10
  }
}
```

---

### **Webhooks**

```http
POST /stripe/webhook
Headers: 
  Stripe-Signature: t=1234567890,v1=abc123...
Body: {
  "id": "evt_1abc123",
  "type": "checkout.session.completed",
  "data": {
    "object": {
      "id": "cs_test_a1b2c3",
      "payment_status": "paid",
      "amount_total": 4800000
    }
  }
}
Response: 200 OK
```

**Handled Events:**
- `checkout.session.completed` → Confirm booking
- `checkout.session.expired` → Mark as expired (future enhancement)

---

## 🚀 Setup & Installation

### **Prerequisites**
- **Java 21+** (JDK 21 recommended)
- **PostgreSQL 18.1+** (or compatible version)
- **Maven 3.8+**
- **Stripe Account** (free test mode)
- **Calendarific API Keys** (6 free tier keys recommended)
- **Gmail Account** (for SMTP)

---

### **1. Clone Repository**
```bash
git clone https://github.com/nikhil4457/airbnb-backend.git
cd airbnb-backend
```

---

### **2. Database Setup**

**Create Database:**
```sql
CREATE DATABASE airbnb_db;
CREATE USER airbnb_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE airbnb_db TO airbnb_user;

-- Connect to airbnb_db
\c airbnb_db

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO airbnb_user;
```

**Note:** Hibernate will auto-create tables on first run (`ddl-auto: update`)

---

### **3. Configure Environment Variables**

Create `src/main/resources/application-local.yml`:

```yaml
spring:
  application:
    name: airbnb-backend
  
  # Email Configuration
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-gmail-app-password  # NOT your regular password!
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
  
  # Database Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/airbnb_db
    username: airbnb_user
    password: your_db_password
    driver-class-name: org.postgresql.Driver
  
  # JPA/Hibernate Configuration
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update  # Use 'validate' in production!
    show-sql: false     # Set true for debugging
    properties:
      hibernate:
        format_sql: true
        jdbc:
          time_zone: UTC

# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /api/v1

# JWT Configuration
jwt:
  secretKey: your-very-long-256-bit-secret-key-here-minimum-32-characters

# Frontend URL (for CORS and redirects)
frontend:
  url: http://localhost:3000

# Stripe Configuration
stripe:
  secret-key: sk_test_your_stripe_secret_key_here
  webhook:
    secret-key: whsec_your_webhook_signing_secret_here

# Calendarific API Keys (6 keys for rotation)
calendarific:
  secret-key-1: your_calendarific_api_key_1
  secret-key-2: your_calendarific_api_key_2
  secret-key-3: your_calendarific_api_key_3
  secret-key-4: your_calendarific_api_key_4
  secret-key-5: your_calendarific_api_key_5
  secret-key-6: your_calendarific_api_key_6

# Email Template Configuration
email:
  from: your-email@gmail.com
  from-name: AirBnB Clone Support
```

---

### **4. Get API Keys**

#### **Stripe (Payment Processing)**
1. Go to https://dashboard.stripe.com/test/apikeys
2. Copy **Secret key** (starts with `sk_test_`)
3. Go to https://dashboard.stripe.com/test/webhooks
4. Click "Add endpoint"
5. Enter URL: `http://localhost:8080/api/v1/stripe/webhook`
6. Select event: `checkout.session.completed`
7. Copy **Signing secret** (starts with `whsec_`)

#### **Calendarific (Holiday API)**
1. Go to https://calendarific.com/
2. Sign up for free account (1000 requests/month)
3. Create **6 different accounts** (use +aliasing: `yourmail+1@gmail.com`)
4. Copy all 6 API keys

#### **Gmail (SMTP for Emails)**
1. Go to https://myaccount.google.com/security
2. Enable **2-Step Verification**
3. Go to **App Passwords**
4. Generate password for "Mail"
5. Copy 16-character password (use this, NOT your regular Gmail password)

---

### **5. Build & Run**

```bash
# Build project
mvn clean install -DskipTests

# Run application
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Or run with JAR
java -jar target/airbnb-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

**Application will start on:** `http://localhost:8080/api/v1`

---

### **6. Verify Installation**

```bash
# Check health (if actuator is enabled)
curl http://localhost:8080/api/v1/actuator/health

# Access Swagger UI
open http://localhost:8080/api/v1/swagger-ui.html
```

---

### **7. Test Stripe Webhooks Locally**

**Install Stripe CLI:**
```bash
# macOS
brew install stripe/stripe-cli/stripe

# Windows (via Scoop)
scoop install stripe

# Linux
# Download from https://github.com/stripe/stripe-cli/releases
```

**Forward webhooks to localhost:**
```bash
stripe listen --forward-to localhost:8080/api/v1/stripe/webhook

# Copy the webhook signing secret (whsec_...) to application-local.yml
```

**Trigger test event:**
```bash
stripe trigger checkout.session.completed
```

---

## 🔐 Environment Variables (Production)

For **production deployment**, use environment variables instead of YAML:

```bash
# Database
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db-host:5432/airbnb_db
export SPRING_DATASOURCE_USERNAME=airbnb_user
export SPRING_DATASOURCE_PASSWORD=prod_password

# JWT
export JWT_SECRET_KEY=your-production-secret-key-min-32-chars

# Stripe
export STRIPE_SECRET_KEY=sk_live_your_live_key
export STRIPE_WEBHOOK_SECRET_KEY=whsec_your_production_webhook_secret

# Email
export SPRING_MAIL_USERNAME=noreply@yourdomain.com
export SPRING_MAIL_PASSWORD=your_smtp_password

# Calendarific
export CALENDARIFIC_SECRET_KEY_1=key1
export CALENDARIFIC_SECRET_KEY_2=key2
# ... (all 6 keys)

# Frontend URL
export FRONTEND_URL=https://yourdomain.com
```

**Security Best Practices:**
- Never commit `application-local.yml` to Git (add to `.gitignore`)
- Use environment variables in production
- Rotate JWT secret key periodically
- Use Stripe live keys only in production
- Enable HTTPS in production (use reverse proxy like Nginx)

---

## ⏰ Scheduled Jobs

### **1. Expired Booking Cleanup**
```java
@Scheduled(fixedRate = 300000)  // Every 5 minutes
public void cleanupExpiredBookings()
```

**Purpose:**
- Finds bookings in `RESERVED`, `GUESTS_ADDED`, or `PAYMENT_PENDING` status
- Created more than 30 minutes ago
- Releases `reservedCount` from inventory
- Marks bookings as `EXPIRED`

**Query:**
```sql
SELECT * FROM booking 
WHERE booking_status IN ('RESERVED', 'GUESTS_ADDED', 'PAYMENT_PENDING')
  AND created_at < NOW() - INTERVAL '30 minutes'
```

**Process:**
```java
for (Booking booking : expiredBookings) {
    // Release inventory
    inventoryRepository.decrementReservedCount(...);
    
    // Mark as expired
    booking.setBookingStatus(BookingStatus.EXPIRED);
    bookingRepository.save(booking);
}
```

---

### **2. Dynamic Pricing Update**
```java
@Scheduled(cron = "0 */30 * * * *")  // Every 30 minutes at :00 and :30
public void updateAllHotelPrices()
```

**Purpose:**
- Recalculates prices for all active hotels
- Applies surge/urgency/holiday/occupancy multipliers
- Updates `Inventory.price` field
- Updates `HotelMinPrice` table for search optimization

**Process:**
```java
List<Hotel> hotels = hotelRepository.findByActiveTrue();

for (Hotel hotel : hotels) {
    for (Room room : hotel.getRooms()) {
        List<Inventory> inventories = inventoryRepository
            .findByRoomIdAndDateBetween(room.getId(), today, today.plusYears(1));
        
        for (Inventory inv : inventories) {
            BigDecimal newPrice = pricingStrategy.calculatePrice(inv);
            inv.setPrice(newPrice);
        }
        
        inventoryRepository.saveAll(inventories);
    }
    
    // Update HotelMinPrice
    updateHotelMinPrices(hotel);
}
```

**Why Every 30 Minutes?**
- Urgency pricing changes as check-in date approaches
- Occupancy pricing changes as bookings come in
- Holiday prices need periodic refresh
- Balance between accuracy and server load

---

### **3. Startup Price Initialization**
```java
@EventListener(ApplicationReadyEvent.class)
public void initializePricesOnStartup()
```

**Purpose:**
- Runs pricing update once when application starts
- Ensures fresh prices after deployment
- Prevents stale data if server was down

**Note:** Uses same logic as scheduled pricing update

---

## 🎓 Advanced Features

### **1. Pessimistic Locking Strategy**

**Problem:** Race condition when two users book the last room simultaneously

**Solution:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE ...")
List<Inventory> findAndLockAvailableInventory(...);
```

**How It Works:**
```sql
-- Transaction 1 (User A)
BEGIN;
SELECT * FROM inventory WHERE ... FOR UPDATE;  -- Locks rows
UPDATE inventory SET reserved_count = reserved_count + 1;
COMMIT;

-- Transaction 2 (User B) - BLOCKED until Transaction 1 commits
BEGIN;
SELECT * FROM inventory WHERE ... FOR UPDATE;  -- Waits for lock
-- Lock released by Transaction 1
-- Sees updated reserved_count, may fail availability check
```

**Benefits:**
- Database-level concurrency control
- No lost updates
- Serializable transactions

**Trade-offs:**
- Slight performance impact (locking overhead)
- Potential for deadlocks (mitigated by short transactions)

---

### **2. Row Count Validation**

**Problem:** Inventory might be closed mid-transaction, affecting fewer rows than expected

**Solution:**
```java
// Custom UPDATE query
@Modifying
@Query("UPDATE Inventory i SET i.reservedCount = i.reservedCount + :count " +
       "WHERE i.room.id = :roomId AND i.date >= :startDate AND i.date < :endDate " +
       "AND i.closed = false")
int incrementReservedCount(...);

// Validation
int updatedRows = inventoryRepository.incrementReservedCount(...);
int expectedRows = (int) DAYS.between(checkInDate, checkOutDate);

if (updatedRows != expectedRows) {
    throw new InventoryModifiedException(
        "Expected " + expectedRows + " rows, but updated " + updatedRows
    );
}
```

**Why This Matters:**
- Detects if hotel manager closed inventory during booking
- Prevents partial bookings (e.g., only 2 out of 3 nights reserved)
- Ensures data consistency

**Example Scenario:**
```
User tries to book Apr 1-4 (3 nights)
Expected: Update 3 rows (Apr 1, 2, 3)

Meanwhile, hotel manager closes Apr 2-3

Result: UPDATE only affects 1 row (Apr 1)
Row count validation: 1 ≠ 3 → Throw exception, rollback transaction
```

---

### **3. State Machine Validation**

**Problem:** Invalid state transitions (e.g., `CONFIRMED` → `RESERVED`)

**Solution:**
```java
private static final Map<BookingStatus, Set<BookingStatus>> VALID_TRANSITIONS = Map.of(
    BookingStatus.RESERVED, Set.of(BookingStatus.GUESTS_ADDED, BookingStatus.PAYMENT_PENDING, BookingStatus.EXPIRED),
    BookingStatus.GUESTS_ADDED, Set.of(BookingStatus.PAYMENT_PENDING, BookingStatus.EXPIRED),
    BookingStatus.PAYMENT_PENDING, Set.of(BookingStatus.CONFIRMED, BookingStatus.EXPIRED),
    BookingStatus.CONFIRMED, Set.of(BookingStatus.CANCELLED_BY_USER, BookingStatus.CANCELLED_BY_HOTEL_MANAGER)
);

private void validateStatusTransition(BookingStatus from, BookingStatus to) {
    Set<BookingStatus> allowedTransitions = VALID_TRANSITIONS.get(from);
    
    if (allowedTransitions == null || !allowedTransitions.contains(to)) {
        throw new InvalidStateTransitionException(
            "Cannot transition from " + from + " to " + to
        );
    }
}
```

**Benefits:**
- Enforces business rules at code level
- Prevents invalid booking states
- Makes workflow explicit and maintainable

---

### **4. Idempotency Protection**

**Problem:** Stripe may send duplicate webhook events (network retries)

**Solution:**
```java
@Transactional
public void handlePaymentSuccess(String sessionId) {
    Booking booking = bookingRepository.findByPaymentSessionId(sessionId)
        .orElseThrow(() -> new BookingNotFoundException(sessionId));
    
    // Idempotency check
    if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
        log.info("Booking {} already confirmed, skipping duplicate webhook", booking.getId());
        return;  // Early return, no processing
    }
    
    // Proceed with confirmation
    confirmBooking(booking);
}
```

**Why This Matters:**
- Prevents double-charging inventory
- Prevents duplicate email notifications
- Ensures exactly-once processing semantics

**Example:**
```
Webhook 1: Arrives, processes booking, marks CONFIRMED
Webhook 2 (duplicate): Arrives 2 seconds later, sees CONFIRMED, skips
```

---

### **5. Check-in/Check-out Date Logic**

**Problem:** Should checkout date be included in inventory?

**Answer:** **NO** - Checkout date is **exclusive**

**Implementation:**
```sql
-- Inventory query
WHERE inventory.date >= :checkInDate 
  AND inventory.date < :checkOutDate  -- Exclusive!
```

**Example:**
```
Booking: Check-in Mar 15, Check-out Mar 18

Nights calculation:
DAYS.between(Mar 15, Mar 18) = 3 nights ✅ (No +1 needed!)

Inventory affected:
WHERE date >= 'Mar 15' AND date < 'Mar 18'
→ Returns: Mar 15, 16, 17 (3 rows) ✅

Explanation:
- Night 1: Mar 15 → Mar 16 (use inventory for Mar 15)
- Night 2: Mar 16 → Mar 17 (use inventory for Mar 16)
- Night 3: Mar 17 → Mar 18 (use inventory for Mar 17)
- Mar 18: Guest checks out, room available for new booking
```

**Why Exclusive Checkout?**
- Guest leaves on morning of checkout date
- Room can be cleaned and rebooked same day
- Industry standard (all booking platforms use this)

---

### **6. Transaction Management in Abstract Classes**

**Problem:** `@Transactional` on abstract class methods doesn't work

**Explanation:**
- Spring AOP creates proxies for `@Transactional` methods
- Proxies only created for concrete classes with `@Service`/`@Component`
- Abstract classes are never instantiated, so no proxy

**Solution:**
```java
// Abstract class - NO @Transactional
public abstract class AbstractHolidayService {
    
    @Autowired
    protected HolidayRepository holidayRepository;
    
    // This method is NOT transactional on its own
    public boolean isHoliday(LocalDate date, CountryCode country) {
        // holidayRepository.save() uses JPA's built-in transaction
        return holidayRepository.existsById(new HolidayId(date, country));
    }
}

// Concrete class - Add @Transactional if needed
@Service
@Transactional  // All public methods now transactional
public class CalendarificHolidayService extends AbstractHolidayService {
    
    @Override
    public boolean isHoliday(LocalDate date, CountryCode country) {
        // Call external API
        // Cache in database
        return super.isHoliday(date, country);
    }
}
```

**Key Takeaway:**
- Add `@Transactional` on **concrete service classes**, not abstract parents
- Repository methods (like `save()`, `findById()`) are already transactional
- Use `@Transactional` when you need multi-step operations in one transaction

---

## 🧩 Challenges & Solutions

### **Challenge 1: Race Conditions in Concurrent Bookings**

**Problem:**
```
10 rooms available
User A tries to book 5 rooms
User B tries to book 6 rooms (simultaneously)

Without locking:
1. Both read: available = 10
2. Both think they can book
3. Both update inventory
4. Result: 11 rooms booked (overbooking!)
```

**Solution:**
```java
// Pessimistic locking
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<Inventory> findAndLockAvailableInventory(...);

// Atomic UPDATE with row count validation
int rows = inventoryRepository.incrementReservedCount(...);
validateCountOfRows(checkInDate, checkOutDate, rows);
```

**How It Works:**
```
User A's transaction:
1. SELECT ... FOR UPDATE  (locks rows)
2. UPDATE reservedCount + 5
3. COMMIT (releases lock)

User B's transaction:
1. SELECT ... FOR UPDATE  (waits for lock)
2. Lock acquired, sees updated count
3. available = 10 - 5 = 5 (less than requested 6)
4. Validation fails, booking rejected ✅
```

**Key Insight:** Database locks are more reliable than application-level synchronization

---

### **Challenge 2: 30-Minute Expiration Without Leaking Inventory**

**Problem:**
```
User reserves room but never pays
Inventory stuck in "reserved" state forever
Other users can't book
```

**Solution:**
```java
@Scheduled(fixedRate = 300000)  // Every 5 minutes
public void cleanupExpiredBookings() {
    LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
    
    List<Booking> expired = bookingRepository.findExpiredBookings(cutoff);
    
    for (Booking booking : expired) {
        inventoryRepository.decrementReservedCount(...);
        booking.setBookingStatus(BookingStatus.EXPIRED);
    }
}
```

**Alternative Considered (and Rejected):**
- **@Scheduled task per booking:** Too many scheduled tasks, not scalable
- **TTL in database:** PostgreSQL doesn't have built-in TTL
- **Redis expiration:** Adds dependency, eventual consistency issues

**Why Batch Processing Works:**
- Single query finds all expired bookings
- Processes in batch (efficient)
- Runs frequently enough (5 min) without overhead
- Simple, reliable, no external dependencies

---

### **Challenge 3: Hotel Closes Inventory Mid-Booking**

**Problem:**
```
User has confirmed booking: Apr 1-10
Hotel closes inventory: Apr 5-7
Should we:
A) Cancel entire booking?
B) Split into Apr 1-4 + Apr 8-10?
C) Allow partial closure?
```

**Solution:** **Option A - Cancel entire booking**

**Reasons:**
1. **User Experience:** Guest expects continuous stay
   - Split booking disrupts trip
   - Guest may not want fragmented stay
   - Creates confusion and frustration

2. **Simplicity:**
   - No complex partial refund calculations
   - No need to create multiple bookings
   - Clear communication: "Hotel closed, full refund"

3. **Fairness:**
   - Hotel's closure = Hotel's responsibility
   - Guest should not be penalized
   - Full refund compensates for inconvenience

4. **Technical:**
   - Avoiding split booking logic
   - Simpler state management
   - Easier to test and maintain

**Implementation:**
```java
// Find ANY overlapping bookings
List<Booking> affected = bookingRepository.findOverlappingBookings(
    roomId, startDate, endDate
);

for (Booking booking : affected) {
    cancelBooking(booking.getId(), false);  // false = hotel cancelled
    emailService.sendCancellationEmail(booking, "Hotel closed inventory");
}
```

---

### **Challenge 4: Stripe Refund Failures**

**Problem:**
```
Network timeout during refund
Insufficient funds in Stripe account
Payment not settled yet
```

**Question:** Should we throw exception and rollback booking cancellation?

**Answer:** **NO** - Log error, continue with cancellation

**Solution:**
```java
try {
    Refund.create(refundParams);
    log.info("Refund processed successfully");
} catch (StripeException e) {
    log.error("Refund failed for booking {}, manual intervention needed", bookingId, e);
    // DON'T throw - booking already cancelled
}
```

**Reasons:**
1. **Booking is already cancelled** (user's primary concern)
2. **Inventory is already released** (hotel's primary concern)
3. **Refund can be retried** manually via Stripe dashboard
4. **Throwing would leave inconsistent state:**
   - Booking cancelled in DB
   - Inventory released
   - But transaction rolled back? (impossible)

**Manual Intervention Process:**
1. Monitor logs for failed refunds
2. Manually process refund in Stripe dashboard
3. Update booking notes with refund confirmation

**Future Enhancement:**
- Implement retry queue (e.g., with RabbitMQ)
- Alert admin via email/Slack
- Track refund status in separate table

---

### **Challenge 5: Date Range Calculations (Nights vs Days)**

**Problem:**
```
Booking: Check-in Mar 15, Check-out Mar 18
How many nights? How many inventory rows?
```

**Common Mistakes:**
```java
// WRONG: Adding +1
long nights = DAYS.between(checkIn, checkOut) + 1;  // 4 nights ❌

// WRONG: Including checkout date
WHERE date >= checkIn AND date <= checkOut  // 4 rows ❌
```

**Correct Solution:**
```java
// Nights calculation
long nights = DAYS.between(checkIn, checkOut);  // 3 nights ✅

// Inventory query
WHERE date >= checkIn AND date < checkOut  // 3 rows ✅
```

**Explanation:**
```
Check-in: Mar 15 (3 PM)
Check-out: Mar 18 (11 AM)

Nights:
- Night 1: Mar 15 → Mar 16
- Night 2: Mar 16 → Mar 17
- Night 3: Mar 17 → Mar 18
Total: 3 nights

Inventory:
- Mar 15: Room occupied (inventory decremented)
- Mar 16: Room occupied (inventory decremented)
- Mar 17: Room occupied (inventory decremented)
- Mar 18: Room available (guest left, inventory available)
```

**Key Principle:** Checkout date is **exclusive** (guest leaves that day)

---

### **Challenge 6: Transaction Management in Scheduled Jobs**

**Problem:**
```java
@Scheduled(cron = "0 */30 * * * *")
public void updateAllHotelPrices() {
    for (Hotel hotel : hotels) {
        updatePricesForHotel(hotel);  // If this fails, should others rollback?
    }
}
```

**Question:** Should entire job be in one transaction?

**Answer:** **NO** - Each hotel should have its own transaction

**Solution:**
```java
@Scheduled(cron = "0 */30 * * * *")
public void updateAllHotelPrices() {
    List<Hotel> hotels = hotelRepository.findAll();
    
    for (Hotel hotel : hotels) {
        try {
            updatePricesForHotel(hotel);  // Separate transaction
        } catch (Exception e) {
            log.error("Failed to update prices for hotel {}", hotel.getId(), e);
            // Continue with other hotels
        }
    }
}

@Transactional
protected void updatePricesForHotel(Hotel hotel) {
    // Update prices for this hotel
    // If fails, only this hotel's transaction rolls back
}
```

**Reasons:**
1. **Isolation:** One hotel's failure doesn't affect others
2. **Partial Success:** 99 hotels succeed, 1 fails → 99 still updated
3. **Observability:** Clear logs showing which hotel failed
4. **Scalability:** Future enhancement can parallelize per-hotel processing




## 📄 License

This project is licensed under the **MIT License**.

```
MIT License

Copyright (c) 2026 nikhil4457

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🙏 Acknowledgments

- **Spring Boot Team** - Excellent framework and documentation
- **Stripe** - Developer-friendly payment API with great documentation
- **Calendarific** - Comprehensive holiday API supporting all countries
- **PostgreSQL** - Reliable, feature-rich database
- **Stack Overflow Community** - Solutions to countless challenges

---

## 📞 Contact

**Developer:** nikhil4457  
**GitHub:** [github.com/nikhil4457](https://github.com/nikhil4457)  
**LinkedIn:** [linkedin.com/in/nikhil-soni-803aa5293](https://www.linkedin.com/in/nikhil-soni-803aa5293/)    

---

## 📈 Project Stats

- **Lines of Code:** ~5,000+ (Java + SQL)
- **Entities:** 10 core entities
- **API Endpoints:** 30+ RESTful endpoints
- **Database Tables:** 15+ tables (including join tables)
- **External Integrations:** 3 (Stripe, Calendarific, Gmail)
- **Scheduled Jobs:** 3 (pricing, cleanup, startup)

---

## 🎯 Learning Outcomes

This project demonstrates expertise in:

✅ **Spring Boot** ecosystem (Data JPA, Security, Mail, Scheduling)  
✅ **Database Design** (normalization, indexing, constraints)  
✅ **Concurrency Control** (pessimistic locking, row count validation)  
✅ **Payment Integration** (Stripe checkout, webhooks, refunds)  
✅ **State Management** (booking state machine, validation)  
✅ **API Design** (RESTful principles, proper status codes)  
✅ **External API Integration** (Calendarific, error handling, retry logic)  
✅ **Email Services** (SMTP, HTML templates, Thymeleaf)  
✅ **Scheduled Tasks** (cron expressions, batch processing)  
✅ **Transaction Management** (ACID properties, rollback strategies)  
✅ **Security** (JWT, BCrypt, role-based access control)  
✅ **Documentation** (comprehensive README, Swagger UI)  

---

**⭐ If you found this project helpful, please give it a star on GitHub!**

---

**Happy Coding! 🚀**
