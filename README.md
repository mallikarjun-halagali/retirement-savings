# BlackRock Retirement Micro-Savings Challenge

**Author:** Mallikarjun Halagali  
**Docker Image:** `blk-hacking-ind-mallikarjun-halagali`  
**Port:** `5477`

---

## Overview

Production-grade Java API for automated retirement savings through expense-based micro-investments. The system rounds up each expense to the next multiple of ₹100 and invests the difference ("remanent"), with support for temporal period overrides (q/p/k rules), NPS & Index Fund return calculations, Indian tax benefit computation, and inflation-adjusted projections.

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.3 |
| Build Tool | Maven 3.9 |
| Container | Docker (Alpine Linux) |
| Port | 5477 |

## Quick Start

### Docker (Recommended)

```bash
# Build and run
docker build -t blk-hacking-ind-mallikarjun-halagali .
docker run -d -p 5477:5477 blk-hacking-ind-mallikarjun-halagali

# Or with Docker Compose
docker compose up --build
```

### Local Development

```bash
# Requires Java 21 and Maven 3.9+
mvn clean package
java -jar target/retirement-savings-1.0.0.jar

# Or run directly
mvn spring-boot:run
```

The server starts on **http://localhost:5477**.

---

## API Endpoints

Base path: `/blackrock/challenge/v1`

### 1. POST `/transactions:parse`

Enriches expenses with ceiling and remanent fields.

**Request:**
```json
{
  "expenses": [
    { "date": "2023-10-01 20:15:00", "amount": 1519 }
  ]
}
```

**Response:**
```json
{
  "transactions": [
    { "date": "2023-10-01 20:15:00", "amount": 1519, "ceiling": 1600, "remanent": 81 }
  ]
}
```

### 2. POST `/transactions:validator`

Validates transactions (amount range, date format, duplicate detection).

**Request:**
```json
{
  "expenses": [
    { "date": "2023-10-01 20:15:00", "amount": 375 },
    { "date": "2023-10-01 20:15:00", "amount": -10 }
  ],
  "wage": 50000
}
```

**Response:**
```json
{
  "valid": [{ "date": "2023-10-01 20:15:00", "amount": 375 }],
  "invalid": [{ "date": "2023-10-01 20:15:00", "amount": -10 }]
}
```

### 3. POST `/transactions:filter`

Applies q (fixed override), p (extra addition), and k (grouping) period rules.

**Request:**
```json
{
  "transactions": [
    { "date": "2023-02-28 15:49:20", "amount": 375 },
    { "date": "2023-07-01 21:59:00", "amount": 620 }
  ],
  "q": [{ "fixed": 0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59" }],
  "p": [{ "extra": 25, "start": "2023-10-01 08:00:00", "end": "2023-12-31 19:59:59" }],
  "k": [{ "start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59" }]
}
```

**Response:**
```json
{
  "savingsByDates": [25],
  "totalSavings": 25
}
```

### 4. POST `/returns:nps`

Calculates NPS returns (7.11% compounded annually) with Indian tax benefit.

**Request:**
```json
{ "invested": 280, "wage": 50000, "age": 29, "inflation": 5.5 }
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

### 5. POST `/returns:index`

Calculates NIFTY 50 Index Fund returns (14.49% compounded annually).

**Request:**
```json
{ "invested": 280, "age": 29, "inflation": 5.5 }
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

### 6. GET `/performance`

Returns system performance metrics.

**Response:**
```json
{
  "executionTimeMs": 40563,
  "memoryUsageMb": 28.75,
  "activeThreads": 16
}
```

---

## Processing Rules

### Auto-Saving Strategy
Each expense is rounded up to the next multiple of ₹100. The difference (remanent) is saved.

### Period Processing Order
1. **Step 1:** Calculate ceiling and remanent
2. **Step 2:** Apply q-period rules (replace remanent with fixed amount; latest start wins)
3. **Step 3:** Apply p-period rules (add extra to remanent; all matching periods stack)
4. **Step 4:** Group by k-periods (sum remanents independently per range)

### Indian Tax Slabs (Simplified)
| Income Range | Rate |
|---|---|
| ₹0 – ₹7,00,000 | 0% |
| ₹7,00,001 – ₹10,00,000 | 10% |
| ₹10,00,001 – ₹12,00,000 | 15% |
| ₹12,00,001 – ₹15,00,000 | 20% |
| Above ₹15,00,000 | 30% |

### NPS Tax Benefit
Eligible deduction = min(invested, 10% of wage, ₹2,00,000)  
Benefit = Tax(wage) − Tax(wage − deduction)

---

## Testing

```bash
# Run all unit tests
mvn test
```

24 unit tests covering:
- Expense rounding (ceiling/remanent)
- Transaction validation (amount, date, duplicates)
- q-period override rules (latest start, first-in-list tiebreaker)
- p-period addition rules (stacking)
- k-period grouping (overlapping ranges)
- Indian tax slab calculation
- NPS tax benefit
- NPS & Index Fund compound interest returns

---

## Project Structure

```
retirement-savings/
├── Dockerfile
├── compose.yaml
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/blackrock/challenge/
    │   ├── RetirementSavingsApplication.java
    │   ├── controller/
    │   │   ├── ChallengeController.java
    │   │   └── GlobalExceptionHandler.java
    │   ├── dto/          (11 request/response DTOs)
    │   ├── model/        (5 data models)
    │   └── service/
    │       ├── TransactionService.java
    │       ├── ReturnsService.java
    │       ├── TaxService.java
    │       └── PerformanceService.java
    └── test/java/com/blackrock/challenge/
        └── SavingsCalculatorTest.java
```

---

## Dependencies

- **Java 21** (JDK for local development, JRE included in Docker image)
- **Maven 3.9+** (for local builds)
- **Docker** (for containerized deployment)

No external database or service dependencies required.
