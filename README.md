# BlackRock Retirement Micro-Savings Challenge

Author: Mallikarjun Halagali  
Docker Image: `blk-hacking-ind-mallikarjun-halagali`  
Port: `5477`

---

## Overview

This is a RESTful API service for automated retirement savings through expense-based micro-investments. The system implements an auto-saving strategy where every expense is rounded up to the next multiple of ₹100, and the spare change ("remanent") is automatically set aside for retirement. The API also supports temporal override rules, investment return projections (NPS and Index Fund), and Indian tax benefit calculations.

---

## Technology Stack and Design Decisions

| What | Used | Why |
|------|------|-----|
| Language | Java 21 | Required by the challenge. LTS version with good performance for handling up to 10^6 transactions. |
| Framework | Spring Boot 3.2.3 | Used for building the REST API endpoints. Handles HTTP routing, JSON parsing, and runs an embedded Tomcat server. |
| Build Tool | Maven 3.9 | Manages project dependencies and builds the JAR. Works well with Docker multi-stage builds. |
| Container OS | Alpine Linux | Keeps the Docker image small (~200MB instead of ~400MB with Ubuntu). |
| Money types | `long` | All amounts are integers in the problem, so `long` avoids floating-point rounding errors. |
| Date parsing | Strict + Lenient | Strict for validating transaction dates. Lenient for period boundaries to handle edge cases like "Nov 31" rolling to Dec 1. |
| Error handling | `@RestControllerAdvice` | Returns JSON error responses (400/500) instead of default HTML error pages. |

---

## Architecture and Workflow

### How the Savings Engine Works

```
┌─────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Expense   │ ──> │  Step 1:     │ ──> │  Step 2:     │ ──> │  Step 3:     │
│   Input     │     │  Round Up    │     │  q-Override  │     │  p-Addition  │
│  ₹1519      │     │  ₹1519→₹1600 │     │  Replace or  │     │  Add extra   │
│             │     │  rem = ₹81   │     │  keep rem    │     │  from p rule │
└─────────────┘     └──────────────┘     └──────────────┘     └──────┬───────┘
                                                                     │
                    ┌──────────────┐     ┌──────────────┐            │
                    │  Step 5:     │ <── │  Step 4:     │ <──────────┘
                    │  Calculate   │     │  k-Grouping  │
                    │  Investment  │     │  Sum by date  │
                    │  Returns     │     │  ranges      │
                    └──────────────┘     └──────────────┘
```

### Processing Pipeline

1. Parse — Each expense amount is rounded up to the next ₹100. The difference is the "remanent" (savings).
   - Example: ₹1519 rounds up to ₹1600, so the remanent is ₹81.
   - If the amount is already a multiple of 100 (like ₹500), the remanent is ₹0.

2. q-Period Override — During certain date ranges, the calculated remanent gets replaced with a fixed amount.
   - If multiple q-periods overlap for a transaction, the one with the latest start date takes priority.
   - If two q-periods have the same start date, the first one in the input list is used.

3. p-Period Addition — During certain date ranges, an extra amount is added on top of the remanent.
   - If multiple p-periods overlap, all their extras are summed together (they stack).
   - This is applied after q-rules. So if q sets a remanent to 50 and p adds 20, the result is 70.

4. k-Period Grouping — At year-end, the remanents are summed up for each k date range independently.
   - A single transaction can fall into multiple k-periods if those ranges overlap.

5. Investment Returns — The total savings are projected into the future using either NPS or Index Fund returns with compound interest.

### Investment Options

| Option | Annual Return | Tax Benefit | Compounding |
|--------|-------------|-------------|-------------|
| NPS (National Pension Scheme) | 7.11% | Yes (Section 80CCD) | Annual |
| NIFTY 50 Index Fund | 14.49% | No | Annual |

Return formula: `A = P × (1 + r)^t` where t = 60 − current age  
Inflation adjustment: `A_real = A / (1 + inflation)^t`

### Indian Tax Slabs (Simplified New Regime)

| Income Range | Tax Rate |
|---|---|
| ₹0 – ₹7,00,000 | 0% |
| ₹7,00,001 – ₹10,00,000 | 10% |
| ₹10,00,001 – ₹12,00,000 | 15% |
| ₹12,00,001 – ₹15,00,000 | 20% |
| Above ₹15,00,000 | 30% |

NPS tax benefit: Eligible deduction = `min(invested, 10% of wage, ₹2,00,000)`  
Benefit = `Tax(wage) − Tax(wage − deduction)`

---

## Quick Start

### Using Docker (Recommended)

```bash
# Build the image
docker build -t blk-hacking-ind-mallikarjun-halagali .

# Run the container
docker run -d -p 5477:5477 blk-hacking-ind-mallikarjun-halagali

# Or use Docker Compose
docker compose up --build
```

### Running Locally

Prerequisites: Java 21 and Maven 3.9+ installed.

```bash
# Build and run
mvn clean package
java -jar target/retirement-savings-1.0.0.jar

# Or run directly with Maven
mvn spring-boot:run
```

The server starts at http://localhost:5477.

---

## RESTful API Endpoints

Base path: `/blackrock/challenge/v1`

### 1. Parse Transactions

`POST /transactions:parse`

Takes raw expenses and enriches them with ceiling and remanent values.

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

Response:
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

Checks each expense against the rules: amount must be between 0 and 500000, date must be valid, and no two transactions can share the same timestamp.

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

Response:
```json
{
  "valid": [{ "date": "2023-10-01 20:15:00", "amount": 375 }],
  "invalid": [{ "date": "2023-10-01 20:15:00", "amount": -10 }]
}
```

---

### 3. Filter and Calculate Savings

`POST /transactions:filter`

Applies the q, p, and k temporal rules and returns the savings grouped by each k-period.

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

Response:
```json
{
  "savingsByDates": [145, 75],
  "totalSavings": 220
}
```

---

### 4. NPS Returns

`POST /returns:nps`

Projects NPS investment returns at 7.11% annual compounding, including the Indian tax benefit calculation.

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/returns:nps \
  -H "Content-Type: application/json" \
  -d '{ "invested": 280, "wage": 50000, "age": 29, "inflation": 5.5 }'
```

Response:
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

Projects NIFTY 50 Index Fund returns at 14.49% annual compounding. There are no tax benefits for this option.

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/returns:index \
  -H "Content-Type: application/json" \
  -d '{ "invested": 280, "age": 29, "inflation": 5.5 }'
```

Response:
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

Returns runtime system metrics like uptime, memory usage, and active thread count.

```bash
curl http://localhost:5477/blackrock/challenge/v1/performance
```

Response:
```json
{
  "executionTimeMs": 40563,
  "memoryUsageMb": 28.75,
  "activeThreads": 16
}
```

---

## Testing

Run all 24 unit tests:

```bash
mvn test
```

### Test Coverage

| Area | Tests | What is Validated |
|------|-------|-------------------|
| Parsing | 4 | Basic rounding, multiples of 100, zero amount, small amounts |
| Validation | 3 | Valid expense, invalid amount (500K+), duplicate timestamps |
| q-Rules | 3 | Fixed override, latest start date priority, same-start tiebreaker |
| p-Rules | 2 | Single extra addition, multiple extras stacking |
| q + p Combined | 1 | q replaces first, then p adds on top |
| k-Grouping | 3 | Multiple ranges, overlapping ranges, empty expense list |
| Tax Slabs | 4 | Below threshold, single slab, multiple slabs, highest slab |
| NPS Benefit | 1 | Deduction eligibility and tax savings |
| Returns | 2 | NPS and Index Fund compound interest with inflation |

---

## Project Structure

```
retirement-savings/
├── Dockerfile                 # Multi-stage build (Maven build → Alpine JRE runtime)
├── compose.yaml               # Docker Compose configuration
├── pom.xml                    # Maven dependencies and build config
├── README.md
├── .gitignore
├── .dockerignore
└── src/
    ├── main/
    │   ├── java/com/blackrock/challenge/
    │   │   ├── RetirementSavingsApplication.java    # Spring Boot entry point
    │   │   ├── controller/
    │   │   │   ├── ChallengeController.java         # All 6 REST endpoints
    │   │   │   └── GlobalExceptionHandler.java      # Centralised error handling
    │   │   ├── dto/                                 # Request and response objects
    │   │   │   ├── ParseRequest.java / ParseResponse.java
    │   │   │   ├── ValidatorRequest.java / ValidatorResponse.java
    │   │   │   ├── FilterRequest.java / FilterResponse.java
    │   │   │   ├── NpsRequest.java / NpsResponse.java
    │   │   │   ├── IndexRequest.java / IndexResponse.java
    │   │   │   └── PerformanceResponse.java
    │   │   ├── model/                               # Domain models
    │   │   │   ├── Expense.java
    │   │   │   ├── Transaction.java
    │   │   │   ├── QPeriod.java
    │   │   │   ├── PPeriod.java
    │   │   │   └── KPeriod.java
    │   │   └── service/                             # Core business logic
    │   │       ├── TransactionService.java          # Parsing, validation, filtering
    │   │       ├── ReturnsService.java              # NPS and Index Fund calculations
    │   │       ├── TaxService.java                  # Indian tax slab calculator
    │   │       └── PerformanceService.java          # Runtime metrics
    │   └── resources/
    │       └── application.properties               # Server port = 5477
    └── test/java/com/blackrock/challenge/
        └── SavingsCalculatorTest.java               # 24 unit tests
```

---

## Dependencies

| Dependency | Purpose |
|-----------|---------|
| `spring-boot-starter-web` | REST API framework with embedded Tomcat server |
| `spring-boot-starter-test` | JUnit 5, Mockito, and Spring test utilities |
| `jackson` (bundled with Spring) | JSON serialization and deserialization |

No external database or third-party service is required. The application is fully self-contained.

---

## Docker Details

| Property | Value |
|----------|-------|
| Build image | `maven:3.9-eclipse-temurin-21` |
| Runtime image | `eclipse-temurin:21-jre-alpine` |
| Exposed port | 5477 |
| Image name | `blk-hacking-ind-mallikarjun-halagali` |
| Final container size | ~200MB |

### Multi-Stage Build

The Dockerfile uses a two-stage approach:

1. Stage 1 (Build) — Uses the full Maven + JDK image (~800MB) to compile the source code and package a JAR.
2. Stage 2 (Runtime) — Copies only the JAR into a lightweight Alpine JRE image (~200MB).

This brings the final image size down by about 75% compared to shipping the full JDK.
