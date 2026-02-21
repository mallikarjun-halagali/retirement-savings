# Retirement Micro-Savings API

## Overview

A RESTful API for a retirement micro-savings system built with Java 21 and Spring Boot 3.2. The system rounds up everyday expenses to the nearest ₹100 and channels the difference ("remanent") into retirement savings instruments (NPS, Index Funds), applying temporal period rules (q/p/k) for flexible savings management.

## Technology Stack and Design Decisions

| What | Used | Why |
|------|------|-----|
| Java 21 | Runtime & language | LTS release with virtual threads and modern syntax for high-throughput request handling |
| Spring Boot 3.2.3 | Web framework | Embedded Tomcat, JSON serialization, dependency injection, and testing utilities in one package |
| Maven 3.9 | Build tool | Standardizes dependency management, produces reproducible builds across environments |
| Alpine Linux | Docker base OS | ~5MB base image reduces container size and attack surface vs Debian/Ubuntu (~200MB vs ~400MB) |
| Docker multi-stage | Build strategy | Separates build (full JDK + Maven) from runtime (JRE only) for minimal production images |
| `double` for money | Numeric type | API spec requires double-typed amounts matching the challenge input/output format |

## Quick Start

### Docker (Recommended)

```bash
# Build the image
docker build -t blk-hacking-ind-mallikarjun-halagali .

# Run on port 5477
docker run -d -p 5477:5477 blk-hacking-ind-mallikarjun-halagali

# Or use Docker Compose
docker compose up -d
```

### Local Development

```bash
# Requires: Java 21, Maven 3.9+
mvn clean package
java -jar target/retirement-savings-1.0.0.jar

# Or run directly
mvn spring-boot:run
```

### Run Tests

```bash
mvn test
```

## API Endpoints

Base URL: `http://localhost:5477/blackrock/challenge/v1`

---

### 1. POST `/transactions:parse`

Enriches raw expenses with ceiling and remanent values.

**Input:** Plain JSON array

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/transactions:parse \
  -H "Content-Type: application/json" \
  -d '[
    {"date": "2023-10-12 20:15:30", "amount": 250},
    {"date": "2023-02-28 15:49:20", "amount": 375}
  ]'
```

**Output:**

```json
[
  {"date": "2023-10-12 20:15:30", "amount": 250.0, "ceiling": 300.0, "remanent": 50.0},
  {"date": "2023-02-28 15:49:20", "amount": 375.0, "ceiling": 400.0, "remanent": 25.0}
]
```

---

### 2. POST `/transactions:validator`

Validates transactions and returns valid/invalid lists with error messages.

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/transactions:validator \
  -H "Content-Type: application/json" \
  -d '{
    "wage": 50000,
    "transactions": [
      {"date": "2023-01-15 10:30:00", "amount": 2000, "ceiling": 300, "remanent": 50},
      {"date": "2023-07-10 09:15:00", "amount": -250, "ceiling": 200, "remanent": 30}
    ]
  }'
```

**Output:**

```json
{
  "valid": [
    {"date": "2023-01-15 10:30:00", "amount": 2000.0, "ceiling": 300.0, "remanent": 50.0}
  ],
  "invalid": [
    {"date": "2023-07-10 09:15:00", "amount": -250.0, "ceiling": 200.0, "remanent": 30.0, "message": "Negative amounts are not allowed"}
  ]
}
```

---

### 3. POST `/transactions:filter`

Validates transactions against q, p, k period rules. Returns enriched valid transactions with `inKPeriod` flag and invalid transactions with error messages.

**Period Rules:**
- **q-period:** Overrides remanent with a fixed value (latest start date wins if multiple overlap)
- **p-period:** Adds extra amount to remanent (all overlapping p-periods stack)
- **k-period:** Determines if a transaction falls within a savings window (`inKPeriod` flag)

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/transactions:filter \
  -H "Content-Type: application/json" \
  -d '{
    "q": [{"fixed": 0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
    "p": [{"extra": 30, "start": "2023-10-01 00:00:00", "end": "2023-12-31 23:59:59"}],
    "k": [{"start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59"}],
    "wage": 50000,
    "transactions": [
      {"date": "2023-02-28 15:49:20", "amount": 375},
      {"date": "2023-07-15 10:30:00", "amount": 620},
      {"date": "2023-10-12 20:15:30", "amount": 250},
      {"date": "2023-10-12 20:15:30", "amount": 250},
      {"date": "2023-12-17 08:09:45", "amount": -480}
    ]
  }'
```

**Output:**

```json
{
  "valid": [
    {"date": "2023-02-28 15:49:20", "amount": 375.0, "ceiling": 400.0, "remanent": 25.0, "inKPeriod": true},
    {"date": "2023-10-12 20:15:30", "amount": 250.0, "ceiling": 300.0, "remanent": 80.0, "inKPeriod": true}
  ],
  "invalid": [
    {"date": "2023-10-12 20:15:30", "amount": 250.0, "message": "Duplicate transaction"},
    {"date": "2023-12-17 08:09:45", "amount": -480.0, "message": "Negative amounts are not allowed"}
  ]
}
```

---

### 4. POST `/returns:nps`

Calculates NPS (National Pension System) returns with tax benefit, grouped by k-periods. Uses 7.11% annual return rate.

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/returns:nps \
  -H "Content-Type: application/json" \
  -d '{
    "age": 29, "wage": 50000, "inflation": 5.5,
    "q": [{"fixed": 0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
    "p": [{"extra": 25, "start": "2023-10-01 08:00:00", "end": "2023-12-31 19:59:59"}],
    "k": [
      {"start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59"},
      {"start": "2023-03-01 00:00:00", "end": "2023-11-31 23:59:59"}
    ],
    "transactions": [
      {"date": "2023-02-28 15:49:20", "amount": 375},
      {"date": "2023-07-01 21:59:00", "amount": 620},
      {"date": "2023-10-12 20:15:30", "amount": 250},
      {"date": "2023-12-17 08:09:45", "amount": 480},
      {"date": "2023-12-17 08:09:45", "amount": -10}
    ]
  }'
```

**Output:**

```json
{
  "totalTransactionAmount": 1725.0,
  "totalCeiling": 1900.0,
  "savingsByDates": [
    {"start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59", "amount": 145.0, "profit": 86.88, "taxBenefit": 0.0},
    {"start": "2023-03-01 00:00:00", "end": "2023-11-31 23:59:59", "amount": 75.0, "profit": 44.94, "taxBenefit": 0.0}
  ]
}
```

---

### 5. POST `/returns:index`

Calculates Index Fund (NIFTY 50) returns, grouped by k-periods. Uses 14.49% annual return rate. Tax benefit is always 0.

Same input format as `/returns:nps`. Same output format with `taxBenefit: 0.0`.

---

### 6. GET `/performance`

Reports system execution metrics.

```bash
curl http://localhost:5477/blackrock/challenge/v1/performance
```

**Output:**

```json
{
  "time": "1970-01-01 00:11:54.135",
  "memory": "25.11",
  "threads": 16
}
```

## Processing Pipeline

```
Input Expenses → Ceiling Rounding (↑100) → Remanent Calculation
    → q-period Override → p-period Addition → k-period Grouping
    → Compound Interest (NPS 7.11% / Index 14.49%)
    → Inflation Adjustment → Tax Benefit (NPS only)
```

## Project Structure

```
retirement-savings/
├── Dockerfile                          # Multi-stage build (build command on line 1)
├── compose.yaml                        # Docker Compose configuration
├── pom.xml                             # Maven dependencies
├── src/
│   ├── main/java/com/blackrock/challenge/
│   │   ├── RetirementSavingsApplication.java
│   │   ├── controller/
│   │   │   ├── ChallengeController.java      # All REST endpoints
│   │   │   └── GlobalExceptionHandler.java   # Error handling
│   │   ├── dto/                              # Request/Response objects
│   │   ├── model/                            # Domain models
│   │   └── service/                          # Business logic
│   │       ├── TransactionService.java       # Parse, validate, filter
│   │       ├── ReturnsService.java           # NPS/Index calculations
│   │       ├── TaxService.java               # Indian tax slabs
│   │       └── PerformanceService.java       # System metrics
│   ├── main/resources/application.properties # Port 5477
│   └── test/java/com/blackrock/challenge/
│       └── SavingsCalculatorTest.java        # 19 unit tests
└── README.md
```

## Test Summary

Tests are located in `src/test/` and can be run with `mvn test`.

| Category | Tests | What's Validated |
|----------|-------|-----------------|
| Parse | 4 | Ceiling rounding, edge cases (0, 1, multiples of 100) |
| Validator | 4 | Valid pass-through, negative rejection, duplicate detection, max amount |
| Filter | 4 | q-period exclusion, p-period addition, full sample validation, inKPeriod flag |
| Returns | 2 | NPS exact values (145→86.88 profit), Index taxBenefit=0 |
| Tax | 5 | All Indian tax slabs, NPS benefit calculation |
| **Total** | **19** | |
