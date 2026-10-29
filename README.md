# Split Bill API

A RESTful API developed using **Java 21, Spring Boot, and Maven** for managing shared expenses among groups of people and calculating settlements efficiently.

This project was developed as part of the **Allo Bank Backend Technical Challenge**.

## 1. Project Overview

The Split Bill API allows users to create groups, register shared expenses, and automatically calculate how much each participant owes or should receive.

The application uses an equal-splitting strategy with a greedy settlement algorithm to simplify transactions between participants.

All monetary calculations use `BigDecimal` to maintain precision, and all expenses are represented in **Indonesian Rupiah (IDR)**.

## 2. Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Programming language and target runtime |
| Spring Boot | REST API framework |
| Maven | Build and dependency management |
| JUnit 5 | Unit testing |
| Docker | Containerization and deployment |
| In-memory HashMap | Temporary group and expense storage |

## 3. Features

- Create a bill group with multiple participants.
- Retrieve group details by ID.
- Add expenses with a specified payer and beneficiaries.
- Automatically divide expenses equally among selected participants.
- Handle rounding remainders in whole rupiah without losing money.
- Calculate individual balances.
- Generate settlement transactions using a greedy algorithm.
- Calculate personalized service charges based on a GitHub username.
- Validate incoming requests and return appropriate HTTP error responses.

## 4. Project Structure

```text
splitbill/
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/com/example/splitbill/
│   │   │   ├── controller/
│   │   │   │   └── BillController.java
│   │   │   ├── dto/
│   │   │   │   └── BillDtos.java
│   │   │   ├── exception/
│   │   │   │   └── ApiExceptionHandler.java
│   │   │   ├── service/
│   │   │   │   └── BillService.java
│   │   │   └── SplitbillApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/example/splitbill/
│           ├── service/BillServiceTest.java
│           └── SplitbillApplicationTests.java
├── Dockerfile
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

## 5. How to Build and Run

### Prerequisites

- Java 21 or higher, compatible with the configured Maven target
- Maven or the included Maven Wrapper
- Docker (optional for local execution)

### Run Locally

Clone the repository:

```bash
git clone https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY.git
cd YOUR_REPOSITORY
```

Build the application:

```bash
./mvnw clean package
```

Run the application:

```bash
./mvnw spring-boot:run
```

For Windows PowerShell:

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

The server runs on:

`http://localhost:4110`

### Run with Docker

Build the Docker image:

```bash
docker build -t splitbill .
```

Run the container:

```bash
docker run --name splitbill-api -p 4110:4110 splitbill
```

The application starts automatically on port **4110**, as required by the challenge.

## 6. API Documentation

Base URL:

`http://localhost:4110/api/groups`

### 6.1 Create a Group

**POST** `/api/groups`

Creates a new group containing multiple participants.

Request:

```bash
curl -X POST http://localhost:4110/api/groups \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jakarta Trip",
    "participants": ["Alice", "Bob", "Charlie"]
  }'
```

Example response (`201 Created`):

```json
{
  "id": "generated-group-uuid",
  "name": "Jakarta Trip",
  "participants": ["Alice", "Bob", "Charlie"],
  "expenseCount": 0
}
```

The `id` value in an actual response will be a generated UUID.

### 6.2 Retrieve Group Details

**GET** `/api/groups/{groupId}`

Retrieves information about an existing group.

```bash
curl http://localhost:4110/api/groups/GROUP_ID
```

Replace `GROUP_ID` with the actual UUID returned during group creation.

### 6.3 Add an Expense

**POST** `/api/groups/{groupId}/expenses`

Records an expense and specifies who paid and which participants share the cost.

```bash
curl -X POST http://localhost:4110/api/groups/GROUP_ID/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Dinner",
    "paidBy": "Alice",
    "amount": 90000,
    "splitAmong": ["Alice", "Bob", "Charlie"]
  }'
```

Add another expense:

```bash
curl -X POST http://localhost:4110/api/groups/GROUP_ID/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Transport",
    "paidBy": "Bob",
    "amount": 60000,
    "splitAmong": ["Alice", "Bob", "Charlie"]
  }'
```

The total group expense is now Rp150.000.

### 6.4 Retrieve Settlement Summary

**GET** `/api/groups/{groupId}/settlement`

Calculates each participant's balance and generates the required settlement transactions.

```bash
curl http://localhost:4110/api/groups/GROUP_ID/settlement
```

Example response, assuming a 4% service charge:

```json
{
  "groupId": "generated-group-uuid",
  "currency": "IDR",
  "totalExpenses": 150000,
  "service_charge_pct": 4,
  "service_charge_amount": 6000,
  "balances": [
    {
      "participant": "Alice",
      "paid": 90000,
      "share": 50000,
      "balance": 40000
    },
    {
      "participant": "Bob",
      "paid": 60000,
      "share": 50000,
      "balance": 10000
    },
    {
      "participant": "Charlie",
      "paid": 0,
      "share": 50000,
      "balance": -50000
    }
  ],
  "settlements": [
    {
      "from": "Charlie",
      "to": "Alice",
      "amount": 40000
    },
    {
      "from": "Charlie",
      "to": "Bob",
      "amount": 10000
    }
  ]
}
```

The actual service charge percentage depends on the configured GitHub username. The `currency` field is included if the optional currency field has been added to the response DTO.

## 7. Settlement Calculation Logic

The settlement calculation follows three steps.

### Step 1: Calculate Individual Shares

Each expense is divided equally among its selected beneficiaries.

For example, an expense of Rp100.000 shared by three people is distributed as:

| Participant | Share |
|---|---:|
| Alice | Rp33.334 |
| Bob | Rp33.333 |
| Charlie | Rp33.333 |
| Total | Rp100.000 |

Any remainder is distributed one rupiah at a time based on the order of beneficiaries.

### Step 2: Calculate Net Balances

Each participant's net balance is calculated as:

`Net Balance = Total Amount Paid - Total Expense Share`

- Positive balance: Participant should receive money.
- Negative balance: Participant owes money.
- Zero balance: Participant is settled.

### Step 3: Generate Settlement Transactions

The algorithm separates participants into creditors and debtors.

For each debtor, it matches an outstanding creditor and transfers the smaller of the two remaining amounts.

This process continues until all balances are settled.

The implementation uses a deterministic greedy approach that reduces unnecessary intermediary payments, but it does not guarantee the globally minimum possible number of transactions in all cases.

## 8. Personalized Service Charge

The service charge percentage is dynamically calculated using the GitHub username.

The calculation follows the challenge requirement:

1. Convert the GitHub username to lowercase.
2. Sum the Unicode values of all characters.
3. Calculate the remainder after division by 10.

Formula:

`service_charge_pct = sum(unicode_values) % 10`

The service charge amount is calculated as:

`service_charge_amount = total_expenses × service_charge_pct / 100`

For IDR, the result is rounded to the nearest whole rupiah using `RoundingMode.HALF_UP`.

**GitHub Username:** `YOUR_GITHUB_USERNAME`

**Calculated Service Charge:** `YOUR_CALCULATED_PERCENTAGE%`

The service charge is reported separately from the settlement transfers.

## 9. Validation and Error Handling

The API implements validation to prevent invalid requests.

| Scenario | Expected Response |
|---|---|
| Missing group name | 400 Bad Request |
| Empty participant list | 400 Bad Request |
| Duplicate participants | 400 Bad Request |
| Negative or zero expense | 400 Bad Request |
| Unknown payer | 400 Bad Request |
| Unknown beneficiary | 400 Bad Request |
| Expense containing fractional rupiah | 400 Bad Request |
| Group not found | 404 Not Found |

## 10. Unit Testing

Unit tests verify the settlement calculation and service charge logic.

Run the tests:

```bash
./mvnw clean test
```

Windows:

```powershell
.\mvnw.cmd clean test
```

The tests cover:

- Correct balance calculation.
- Correct settlement transactions.
- Service charge percentage and amount.
- Distribution of rounding remainders.

## 11. Design Decisions and Limitations

**In-memory storage:** Group and expense data are stored using Java's `HashMap`. This keeps the implementation lightweight and focuses on the settlement algorithm. Data is lost when the application stops or restarts.

**Currency handling:** All expenses use Indonesian Rupiah (IDR), with whole-rupiah precision.

**Equal split:** Expenses are divided equally among selected beneficiaries. Percentage-based and exact-amount split strategies are not currently implemented.

**Settlement algorithm:** A greedy algorithm creates direct transfers between debtors and creditors. It does not guarantee a globally minimum number of transactions in every possible case.

**Service charge:** The personalized service charge is calculated independently and does not affect participant balances.

Potential improvements include persistent database storage, additional split strategies, payment recording, and transaction history.

## 12. Submission Question

**Question:** What was the hardest design decision you made while building this, and what trade-off did you accept?

**Answer:** 

**Hardest Design Decision:** Choosing between a database and in-memory storage. I chose HashMap to keep the project simple and focus on the expense-splitting and settlement logic.

**Trade-off:** Data is lost when the application restarts. I accepted this limitation to prioritize core functionality and testing. In the future, I would use PostgreSQL for persistent storage.

---

Developed for the Allo Bank Backend Technical Challenge.
