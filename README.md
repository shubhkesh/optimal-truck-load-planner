# Optimal Truck Load Planner

A high-performance microservice that optimizes truck load combinations to maximize carrier revenue while respecting weight, volume, hazmat, and route constraints.

## Problem Statement

This service solves the **multi-order truck optimization problem** for logistics platforms. Given a truck with limited capacity and a set of available orders, it finds the optimal combination that:

- ✅ Maximizes total payout to carrier
- ✅ Respects weight and volume limits
- ✅ Ensures route compatibility (same origin → destination)
- ✅ Handles hazmat restrictions (max 1 hazmat order per load)
- ✅ Validates pickup/delivery time windows

## Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.2.5
- **Build Tool**: Gradle 8.7
- **Algorithm**: Dynamic Programming (Bitmask DP)
- **Testing**: JUnit 5, Mockito, Spring Test
- **Containerization**: Docker, Docker Compose

## Algorithm

The service uses **Bitmask Dynamic Programming** for optimal load selection:

- **Time Complexity**: O(2^n × n) where n = number of orders
- **Space Complexity**: O(2^n)
- **Performance**: Handles up to 22 orders in under 2 seconds
- **Guarantee**: Always finds the globally optimal solution

### How It Works

1. **Filtering**: Pre-filter orders by route compatibility
2. **State Representation**: Each subset of orders is represented as a bitmask
3. **DP Transition**: For each valid state, try adding each remaining order
4. **Constraint Validation**: Check weight, volume, and hazmat rules
5. **Optimization**: Track maximum payout across all valid states

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 21 (if running locally without Docker)

### Running with Docker (Recommended)

```bash
# Clone the repository
git clone <your-repo-url>
cd optimal-truck-load-planner

# Build and start the service
docker compose up --build

# Service will be available at http://localhost:8080
```

### Running Locally

```bash
# Build the project
./gradlew clean build

# Run the application
./gradlew bootRun

# Or run the JAR directly
java -jar build/libs/optimal-truck-load-planner-1.0.0.jar
```

## API Documentation

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP"
}
```

### Optimize Load

**Endpoint:** `POST /api/v1/load-optimizer/optimize`

**Request Example:**

```bash
curl -X POST http://localhost:8080/api/v1/load-optimizer/optimize \
  -H "Content-Type: application/json" \
  -d @sample-request.json
```

Or with inline JSON:

```bash
curl -X POST http://localhost:8080/api/v1/load-optimizer/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "truck": {
      "id": "truck-123",
      "max_weight_lbs": 44000,
      "max_volume_cuft": 3000
    },
    "orders": [
      {
        "id": "ord-001",
        "payout_cents": 250000,
        "weight_lbs": 18000,
        "volume_cuft": 1200,
        "origin": "Los Angeles, CA",
        "destination": "Dallas, TX",
        "pickup_date": "2025-12-05",
        "delivery_date": "2025-12-09",
        "is_hazmat": false
      },
      {
        "id": "ord-002",
        "payout_cents": 180000,
        "weight_lbs": 12000,
        "volume_cuft": 900,
        "origin": "Los Angeles, CA",
        "destination": "Dallas, TX",
        "pickup_date": "2025-12-04",
        "delivery_date": "2025-12-10",
        "is_hazmat": false
      },
      {
        "id": "ord-003",
        "payout_cents": 320000,
        "weight_lbs": 30000,
        "volume_cuft": 1800,
        "origin": "Los Angeles, CA",
        "destination": "Dallas, TX",
        "pickup_date": "2025-12-06",
        "delivery_date": "2025-12-08",
        "is_hazmat": true
      }
    ]
  }'
```

**Response:**

```json
{
  "truck_id": "truck-123",
  "selected_order_ids": ["ord-002", "ord-003"],
  "total_payout_cents": 500000,
  "total_weight_lbs": 42000,
  "total_volume_cuft": 2700,
  "utilization_weight_percent": 95.45,
  "utilization_volume_percent": 90.0
}
```

**Note:** The algorithm selects ord-002 + ord-003 (500,000 cents) instead of ord-001 + ord-002 (430,000 cents) because it maximizes revenue while respecting all constraints. Even though ord-003 is hazmat, it can be combined with non-hazmat orders (only multiple hazmat orders together are prohibited).

### Request Schema

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `truck.id` | string | Yes | Unique truck identifier |
| `truck.max_weight_lbs` | integer | Yes | Maximum weight capacity in pounds |
| `truck.max_volume_cuft` | integer | Yes | Maximum volume capacity in cubic feet |
| `orders` | array | Yes | List of available orders (max 22) |
| `orders[].id` | string | Yes | Unique order identifier |
| `orders[].payout_cents` | integer | Yes | Payout amount in cents |
| `orders[].weight_lbs` | integer | Yes | Order weight in pounds |
| `orders[].volume_cuft` | integer | Yes | Order volume in cubic feet |
| `orders[].origin` | string | Yes | Pickup location |
| `orders[].destination` | string | Yes | Delivery location |
| `orders[].pickup_date` | date | Yes | Pickup date (YYYY-MM-DD) |
| `orders[].delivery_date` | date | Yes | Delivery date (YYYY-MM-DD) |
| `orders[].is_hazmat` | boolean | Yes | Hazardous material flag |

### Response Schema

| Field | Type | Description |
|-------|------|-------------|
| `truck_id` | string | Truck identifier |
| `selected_order_ids` | array | IDs of selected orders |
| `total_payout_cents` | integer | Total revenue in cents |
| `total_weight_lbs` | integer | Total weight of selected orders |
| `total_volume_cuft` | integer | Total volume of selected orders |
| `utilization_weight_percent` | number | Weight utilization percentage |
| `utilization_volume_percent` | number | Volume utilization percentage |

### Error Responses

**400 Bad Request** - Validation errors

```json
{
  "timestamp": "2025-12-05T10:30:00Z",
  "status": 400,
  "message": "Truck ID is required",
  "path": "/api/v1/load-optimizer/optimize"
}
```

**500 Internal Server Error** - Unexpected errors

```json
{
  "timestamp": "2025-12-05T10:30:00Z",
  "status": 500,
  "message": "An unexpected error occurred",
  "path": "/api/v1/load-optimizer/optimize"
}
```

## Testing

### Run All Tests

```bash
./gradlew test
```

### Run with Coverage Report

```bash
./gradlew test jacocoTestReport

# View report at: build/reports/jacoco/test/html/index.html
```

### Coverage Requirements

- **Minimum Coverage**: 85% (enforced by JaCoCo)
- **Unit Tests**: Service and controller layers
- **Integration Tests**: Full API flow with realistic data

### Test Categories

- ✅ **Unit Tests**: Service logic, edge cases, validation
- ✅ **Controller Tests**: API contract, error handling
- ✅ **Integration Tests**: End-to-end flows, performance

## Project Structure

```
optimal-truck-load-planner/
├── src/
│   ├── main/
│   │   ├── java/com/teleport/loadplanner/
│   │   │   ├── controller/          # REST API controllers
│   │   │   ├── service/             # Business logic
│   │   │   ├── model/               # Domain models
│   │   │   ├── exception/           # Error handling
│   │   │   └── LoadPlannerApplication.java
│   │   └── resources/
│   │       └── application.yml      # Configuration
│   └── test/
│       └── java/com/teleport/loadplanner/
│           ├── service/             # Service tests
│           ├── controller/          # Controller tests
│           └── integration/         # Integration tests
├── Dockerfile
├── docker-compose.yml
├── build.gradle
├── sample-request.json
└── README.md
```

## Design Decisions

### Why Bitmask DP?

- **Optimal Solution**: Guarantees finding the best combination
- **Efficient**: Handles 22 orders in < 2 seconds
- **Scalable**: Better than brute force O(2^n × n!) approach

### Why Java 21 + Spring Boot?

- **Modern JVM**: Records, pattern matching, virtual threads
- **Production-Ready**: Battle-tested framework with excellent tooling
- **Enterprise Standard**: Wide adoption in logistics/enterprise domains

### Money as Integer Cents

- **Precision**: Avoids floating-point rounding errors
- **Standard Practice**: Common in financial systems
- **Type Safety**: Uses `long` for large amounts

## Constraints & Limitations

- **Max Orders**: 22 (due to 2^n state space)
- **Route Compatibility**: All orders must share same origin/destination
- **Hazmat Limit**: Maximum 1 hazmat order per load
- **Time Windows**: Simplified validation (pickup ≤ delivery)

## Development

### Build

```bash
./gradlew clean build
```

### Run Tests

```bash
./gradlew test
```

### Code Coverage

```bash
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### Docker Build

```bash
docker build -t optimal-truck-load-planner .
docker run -p 8080:8080 optimal-truck-load-planner
```
