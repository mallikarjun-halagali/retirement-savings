# BlackRock Retirement Micro-Savings Challenge

**Author:** Mallikarjun Halagali  
**Docker Image:** `blk-hacking-ind-mallikarjun-halagali`  
**Port:** `5477`

---

## 📋 Overview

A RESTful API service for automated retirement savings through expense-based micro-investments. The system implements an **auto-saving strategy** — every expense is rounded up to the next multiple of ₹100, and the spare change ("remanent") is automatically invested for retirement. The API also supports temporal override rules, investment return projections (NPS & Index Fund), and Indian tax benefit calculations.

---

## 🏗️ Architecture & Workflow

### How the Savings Engine Works

```
┌─────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Expense   │ ──▶ │  Step 1:     │ ──▶ │  Step 2:     │ ──▶ │  Step 3:     │
│   Input     │     │  Round Up    │     │  q-Override  │     │  p-Addition  │
│  ₹1519      │     │  ₹1519→₹1600│     │  Replace or  │     │  Add extra   │
│             │     │  rem = ₹81   │     │  keep rem    │     │  from p rule │
└─────────────┘     └──────────────┘     └──────────────┘     └──────┬───────┘
                                                                     │
                    ┌──────────────┐     ┌──────────────┐            │
                    │  Step 5:     │ ◀── │  Step 4:     │ ◀──────────┘
                    │  Calculate   │     │  k-Grouping  │
                    │  Investment  │     │  Sum by date  │
                    │  Returns     │     │  ranges      │
                    └──────────────┘     └──────────────┘
```

### Processing Pipeline (Step by Step)

1. **Parse** — Each expense amount is rounded up to the next ₹100. The difference is the "remanent" (savings).
   - Example: ₹1519 → ceiling ₹1600 → **remanent = ₹81**
   - If already a multiple of 100 (e.g., ₹500): **remanent = ₹0**

2. **q-Period Override** — During specific date ranges, the remanent is **replaced** with a fixed amount.
   - If multiple q-periods overlap: the one with the **latest start date** wins.
   - If same start date: the **first one in the input list** wins.

3. **p-Period Addition** — During specific date ranges, an extra amount is **added** to the remanent.
   - If multiple p-periods overlap: **all extras are summed** (they stack).
   - Applied **after** q-rules (so if q sets remanent to 50, p adds on top of 50).

4. **k-Period Grouping** — At the end of the year, remanents are summed for each k date range.
   - A transaction can belong to **multiple k-periods** (overlapping ranges are independent).

5. **Investment Returns** — The total savings can be projected using NPS or Index Fund returns.

### Investment Options

| Option | Annual Return | Tax Benefit | Compounding |
|--------|-------------|-------------|-------------|
| **NPS** (National Pension Scheme) | 7.11% | Yes (Section 80CCD) | Annual |
| **NIFTY 50 Index Fund** | 14.49% | No | Annual |

**Return Formula:** `A = P × (1 + r)^t` where `t = 60 - current_age`  
**Inflation Adjustment:** `A_real = A / (1 + inflation)^t`

### Indian Tax Slabs (Simplified New Regime)

| Income Range | Tax Rate |
|---|---|
| ₹0 – ₹7,00,000 | 0% |
| ₹7,00,001 – ₹10,00,000 | 10% |
| ₹10,00,001 – ₹12,00,000 | 15% |
| ₹12,00,001 – ₹15,00,000 | 20% |
| Above ₹15,00,000 | 30% |

**NPS Tax Benefit:** Eligible deduction = `min(invested, 10% of wage, ₹2,00,000)`  
Benefit = `Tax(wage) − Tax(wage − deduction)`

---

## 🛠️ Technology Stack & Design Decisions

| Component | Choice | Why? |
|-----------|--------|------|
| **Language** | Java 21 | LTS release with virtual threads, pattern matching, and record support. Ideal for financial systems requiring type safety and performance. |
| **Framework** | Spring Boot 3.2.3 | Industry-standard for building RESTful APIs. Auto-configuration reduces boilerplate. Built-in Tomcat server, JSON serialization, and testing support. |
| **Build Tool** | Maven 3.9 | Widely adopted dependency management. Reproducible builds via `pom.xml`. Integrates seamlessly with Docker multi-stage builds. |
| **Container OS** | Alpine Linux | Minimal footprint (~5MB base image). Reduces container size from ~400MB to ~200MB. Smaller attack surface for security. |
| **Data Types** | `long` for money | Avoids floating-point precision issues (₹1519 stored as `1519`, not `15.19`). Since all amounts in the problem are integers, `long` is the natural fit. |
| **Date Parsing** | Lenient + Strict | Strict parsing for transaction validation (reject invalid dates). Lenient parsing for period boundaries (handle edge cases like "Nov 31" → "Dec 1"). |
| **Error Handling** | Global `@RestControllerAdvice` | Centralized error handling returns consistent JSON error responses (400/500) instead of Spring's default HTML error pages. |

---

## 🚀 Quick Start

### Docker (Recommended)

```bash
# Build the image
docker build -t blk-hacking-ind-mallikarjun-halagali .

# Run the container
docker run -d -p 5477:5477 blk-hacking-ind-mallikarjun-halagali

# Or use Docker Compose
docker compose up --build
```

### Local Development

```bash
# Prerequisites: Java 21, Maven 3.9+

# Build and run
mvn clean package
java -jar target/retirement-savings-1.0.0.jar

# Or run directly with Maven
mvn spring-boot:run
```

The server starts on **http://localhost:5477**.

---

## 📡 RESTful API Endpoints

Base path: `/blackrock/challenge/v1`

### 1. Parse Transactions

`POST /transactions:parse`

Enriches raw expenses with ceiling and remanent fields.

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/transactions:parse \
  -H "Content-Type: application/json" \
  -d '{
    "expenses": [
      { "date": "2023-10-01 20:15:00", "amount": 1519 },
      { "date": "2023-10-02 10:00:00", "amount": 250 }
    ]
  }'
```

**Response:**
```json
{
  "transactions": [
    { "date": "2023-10-01 20:15:00", "amount": 1519, "ceiling": 1600, "remanent": 81 },
    { "date": "2023-10-02 10:00:00", "amount": 250, "ceiling": 300, "remanent": 50 }
  ]
}
```

---

### 2. Validate Transactions

`POST /transactions:validator`

Validates expenses against constraints: amount (0 ≤ x < 500000), valid date format, no duplicate timestamps.

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/transactions:validator \
  -H "Content-Type: application/json" \
  -d '{
    "expenses": [
      { "date": "2023-10-01 20:15:00", "amount": 375 },
      { "date": "2023-10-01 20:15:00", "amount": -10 }
    ],
    "wage": 50000
  }'
```

**Response:**
```json
{
  "valid": [{ "date": "2023-10-01 20:15:00", "amount": 375 }],
  "invalid": [{ "date": "2023-10-01 20:15:00", "amount": -10 }]
}
```

---

### 3. Filter & Calculate Savings

`POST /transactions:filter`

Applies q/p/k temporal rules and computes savings per k-period.

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/transactions:filter \
  -H "Content-Type: application/json" \
  -d '{
    "transactions": [
      { "date": "2023-02-28 15:49:20", "amount": 375 },
      { "date": "2023-07-01 21:59:00", "amount": 620 },
      { "date": "2023-10-12 20:15:30", "amount": 250 },
      { "date": "2023-12-17 08:09:45", "amount": 480 }
    ],
    "q": [{ "fixed": 0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59" }],
    "p": [{ "extra": 25, "start": "2023-10-01 08:00:00", "end": "2023-12-31 19:59:59" }],
    "k": [
      { "start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59" },
      { "start": "2023-03-01 00:00:00", "end": "2023-11-31 23:59:59" }
    ]
  }'
```

**Response:**
```json
{
  "savingsByDates": [145, 75],
  "totalSavings": 220
}
```

---

### 4. NPS Returns

`POST /returns:nps`

Calculates NPS investment returns (7.11% annual compounding) with Indian tax benefit.

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/returns:nps \
  -H "Content-Type: application/json" \
  -d '{ "invested": 280, "wage": 50000, "age": 29, "inflation": 5.5 }'
```

**Response:**
```json
{
  "invested": 280.0,
  "returns": 2354.45,
  "profit": 2074.45,
  "taxBenefit": 0.0,
  "inflationAdjusted": 447.78
}
```

---

### 5. Index Fund Returns

`POST /returns:index`

Calculates NIFTY 50 Index Fund returns (14.49% annual compounding). No tax benefits.

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/returns:index \
  -H "Content-Type: application/json" \
  -d '{ "invested": 280, "age": 29, "inflation": 5.5 }'
```

**Response:**
```json
{
  "invested": 280.0,
  "returns": 18576.0,
  "profit": 18296.0,
  "inflationAdjusted": 3532.86
}
```

---

### 6. Performance Metrics

`GET /performance`

Returns runtime system metrics.

```bash
curl http://localhost:5477/blackrock/challenge/v1/performance
```

**Response:**
```json
{
  "executionTimeMs": 40563,
  "memoryUsageMb": 28.75,
  "activeThreads": 16
}
```

---

## 🧪 Testing

```bash
# Run all 24 unit tests
mvn test
```

### Test Coverage

| Area | Tests | What's Validated |
|------|-------|-----------------|
| Parsing | 4 | Basic rounding, multiples of 100, zero, small amounts |
| Validation | 3 | Valid expense, invalid amount (≥500K), duplicate dates |
| q-Rules | 3 | Override, latest start wins, same-start tiebreaker |
| p-Rules | 2 | Single addition, multiple stacking |
| q+p Combined | 1 | q replaces then p adds on top |
| k-Grouping | 3 | Multiple ranges, overlapping ranges, empty expenses |
| Tax | 4 | Below threshold, single slab, multi-slab, all slabs |
| NPS Benefit | 1 | Deduction and benefit calculation |
| Returns | 2 | NPS and Index Fund compound interest |

---

## 📁 Project Structure

```
retirement-savings/
├── Dockerfile                 # Multi-stage build (Maven → Alpine JRE)
├── compose.yaml               # Docker Compose configuration
├── pom.xml                    # Maven dependencies & build config
├── README.md                  # This file
├── .gitignore
├── .dockerignore
└── src/
    ├── main/
    │   ├── java/com/blackrock/challenge/
    │   │   ├── RetirementSavingsApplication.java    # Spring Boot entry point
    │   │   ├── controller/
    │   │   │   ├── ChallengeController.java         # 6 REST endpoints
    │   │   │   └── GlobalExceptionHandler.java      # Error handling (400/500)
    │   │   ├── dto/                                 # Request/Response objects
    │   │   │   ├── ParseRequest/Response.java
    │   │   │   ├── ValidatorRequest/Response.java
    │   │   │   ├── FilterRequest/Response.java
    │   │   │   ├── NpsRequest/Response.java
    │   │   │   ├── IndexRequest/Response.java
    │   │   │   └── PerformanceResponse.java
    │   │   ├── model/                               # Domain models
    │   │   │   ├── Expense.java
    │   │   │   ├── Transaction.java
    │   │   │   ├── QPeriod.java / PPeriod.java / KPeriod.java
    │   │   └── service/                             # Business logic
    │   │       ├── TransactionService.java          # Parse, validate, filter
    │   │       ├── ReturnsService.java              # NPS & Index calculations
    │   │       ├── TaxService.java                  # Indian tax slabs
    │   │       └── PerformanceService.java          # Runtime metrics
    │   └── resources/
    │       └── application.properties               # Server port config
    └── test/java/com/blackrock/challenge/
        └── SavingsCalculatorTest.java               # 24 unit tests
```

---

## 🔧 Dependencies

| Dependency | Purpose |
|-----------|---------|
| `spring-boot-starter-web` | REST API framework with embedded Tomcat |
| `spring-boot-starter-test` | JUnit 5, Mockito, Spring test utilities |
| `jackson` (included via Spring) | JSON serialization/deserialization |

**No external database or third-party service required.** The application is fully self-contained.

---

## 📦 Docker Details

| Property | Value |
|----------|-------|
| Base Image (Build) | `maven:3.9-eclipse-temurin-21` |
| Base Image (Runtime) | `eclipse-temurin:21-jre-alpine` |
| Exposed Port | `5477` |
| Image Name | `blk-hacking-ind-mallikarjun-halagali` |
| Container Size | ~200MB |

### Multi-Stage Build Strategy

```
Stage 1 (Build):  maven:3.9-eclipse-temurin-21 (~800MB)
    └── Compiles source code and packages JAR

Stage 2 (Runtime): eclipse-temurin:21-jre-alpine (~200MB)
    └── Copies only the JAR, runs with minimal JRE
```

This reduces the final image size by **~75%** compared to shipping the full JDK.
