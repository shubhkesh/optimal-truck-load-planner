# Project Summary: Optimal Truck Load Planner

## Overview

Production-ready microservice that optimizes truck load combinations to maximize carrier revenue while respecting weight, volume, hazmat, and route constraints.

## Technical Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.2.5
- **Build Tool:** Gradle 8.7
- **Algorithm:** Dynamic Programming (Bitmask DP)
- **Testing:** JUnit 5, Mockito, Spring Test
- **Coverage:** 85%+ (enforced by JaCoCo)
- **Containerization:** Docker, Docker Compose

## Key Features

### ✅ Optimal Solution
- Bitmask Dynamic Programming algorithm
- Guaranteed to find the globally optimal combination
- Time complexity: O(2^n × n)
- Handles up to 22 orders in < 2 seconds

### ✅ Constraint Handling
- **Weight limit:** Respects truck max weight capacity
- **Volume limit:** Respects truck max volume capacity
- **Hazmat rule:** Maximum 1 hazmat order per load
- **Route compatibility:** All orders must share same origin → destination
- **Time windows:** Validates pickup ≤ delivery dates

### ✅ Production Quality
- Comprehensive input validation
- Global exception handling with structured error responses
- Proper HTTP status codes (200, 400, 500)
- Health check endpoint (`/actuator/health`)
- Structured logging with SLF4J
- Money handled as integer cents (no floating point errors)

### ✅ Testing
- **Unit tests:** Service and controller layers
- **Integration tests:** Full API flow with realistic data
- **Coverage:** 85%+ enforced by JaCoCo
- **Test count:** 24 tests covering all edge cases

### ✅ API Design
- RESTful endpoint: `POST /api/v1/load-optimizer/optimize`
- JSON request/response with snake_case fields
- Validation annotations on all inputs
- Clear error messages

## Project Structure

```
optimal-truck-load-planner/
├── src/
│   ├── main/java/com/teleport/loadplanner/
│   │   ├── controller/          # REST API
│   │   ├── service/             # Business logic (DP algorithm)
│   │   ├── model/               # Domain models
│   │   ├── exception/           # Error handling
│   │   └── LoadPlannerApplication.java
│   └── test/java/com/teleport/loadplanner/
│       ├── service/             # Unit tests
│       ├── controller/          # Controller tests
│       ├── exception/           # Exception handler tests
│       └── integration/         # Integration tests
├── Dockerfile
├── docker-compose.yml
├── README.md                    # Full documentation
├── QUICKSTART.md               # Quick start guide
├── GITHUB_SETUP.md             # GitHub setup instructions
├── sample-request.json         # Example request
└── verify.sh                   # Verification script
```

## Algorithm Details

### Bitmask Dynamic Programming

**State Representation:**
- Each subset of orders is represented as a bitmask
- Bit i = 1 means order i is included
- Example: `101` = orders 0 and 2 are selected

**DP Transition:**
```
For each valid state (mask):
    For each order i not in mask:
        new_mask = mask | (1 << i)
        if valid_combination(new_mask):
            dp[new_mask] = max(dp[new_mask], dp[mask] + order[i].payout)
```

**Validation:**
- Check total weight ≤ truck capacity
- Check total volume ≤ truck capacity
- Check at most 1 hazmat order

**Complexity:**
- Time: O(2^n × n) where n = number of orders
- Space: O(2^n)
- Performance: < 2 seconds for n=22

## Test Coverage

| Component | Coverage |
|-----------|----------|
| LoadOptimizerService | 100% |
| LoadOptimizerController | 100% |
| GlobalExceptionHandler | 100% |
| Overall (excluding DTOs) | 85%+ |

## API Examples

### Success Case
```bash
curl -X POST http://localhost:8080/api/v1/load-optimizer/optimize \
  -H "Content-Type: application/json" \
  -d @sample-request.json
```

Response:
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

### Error Case
```bash
curl -X POST http://localhost:8080/api/v1/load-optimizer/optimize \
  -H "Content-Type: application/json" \
  -d '{"truck": {"id": "", "max_weight_lbs": -1000}}'
```

Response (400):
```json
{
  "timestamp": "2025-12-05T10:30:00Z",
  "status": 400,
  "message": "Truck ID is required, Max weight must be positive",
  "path": "/api/v1/load-optimizer/optimize"
}
```

## Performance Benchmarks

| Orders | Time (ms) | Memory (MB) |
|--------|-----------|-------------|
| 10     | < 50      | ~20         |
| 15     | < 200     | ~30         |
| 20     | < 1000    | ~50         |
| 22     | < 2000    | ~60         |

*Tested on: MacBook Pro M1, 16GB RAM*

## Docker Support

### Build & Run
```bash
docker compose up --build
```

### Multi-stage Build
- Stage 1: Build with Gradle
- Stage 2: Runtime with JRE only
- Final image: ~200MB (Alpine-based)

### Health Check
- Endpoint: `/actuator/health`
- Interval: 30s
- Timeout: 10s
- Retries: 3

## Deployment Readiness

✅ Stateless design (horizontally scalable)
✅ Docker & docker-compose ready
✅ Health check endpoint
✅ Structured logging
✅ Proper error handling
✅ Input validation
✅ No hardcoded values
✅ Configurable via application.yml
✅ Production-grade exception handling

## Future Enhancements

1. **Multi-route optimization:** Handle different origin/destination pairs
2. **Advanced time windows:** Conflict detection for pickup/delivery times
3. **Pareto-optimal solutions:** Return multiple trade-off options
4. **Configurable weights:** Balance revenue vs. utilization
5. **Persistent storage:** Database for historical optimizations
6. **Caching:** Redis for repeated requests
7. **Metrics:** Prometheus/Grafana integration
8. **API versioning:** Support multiple API versions

## Files Checklist

- [x] Source code (Java 21)
- [x] Tests (24 tests, 85%+ coverage)
- [x] Dockerfile
- [x] docker-compose.yml
- [x] README.md (comprehensive)
- [x] QUICKSTART.md
- [x] GITHUB_SETUP.md
- [x] sample-request.json
- [x] .gitignore
- [x] .dockerignore
- [x] verify.sh
- [x] Git repository initialized

## Submission Checklist

- [x] Code compiles and runs
- [x] All tests pass
- [x] Coverage ≥ 85%
- [x] Docker builds successfully
- [x] Health check works
- [x] API returns correct results
- [x] Error handling works
- [x] Documentation complete
- [x] Ready for GitHub

## Contact

For questions or clarifications, please reach out via email or GitHub issues.

---

**Created:** April 24, 2026
**Author:** Shubham Kesharwani
**Purpose:** Teleport Technical Assessment
