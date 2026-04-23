# Quick Start Guide

## Prerequisites

- Docker & Docker Compose installed
- OR Java 21 installed (for local development)

## Option 1: Run with Docker (Recommended)

```bash
# Build and start the service
docker compose up --build

# Service will be available at http://localhost:8080
```

## Option 2: Run Locally

```bash
# Build the project
./gradlew clean build

# Run the application
./gradlew bootRun

# Or run the JAR directly
java -jar build/libs/optimal-truck-load-planner-1.0.0.jar
```

## Verify Installation

### 1. Health Check

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### 2. Test Optimization API

```bash
curl -X POST http://localhost:8080/api/v1/load-optimizer/optimize \
  -H "Content-Type: application/json" \
  -d @sample-request.json
```

Expected response:
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

## Run Tests

```bash
# Run all tests
./gradlew test

# Run tests with coverage report
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

## Stop the Service

### Docker
```bash
docker compose down
```

### Local
Press `Ctrl+C` in the terminal running the application

## Troubleshooting

### Port 8080 already in use
```bash
# Find and kill the process using port 8080
lsof -ti:8080 | xargs kill -9

# Or change the port in docker-compose.yml or application.yml
```

### Docker build fails
```bash
# Clean Docker cache
docker system prune -a

# Rebuild
docker compose up --build
```

### Tests fail
```bash
# Clean and rebuild
./gradlew clean test --info
```

## Next Steps

1. Push to GitHub
2. Share the repository link with Teleport
3. See README.md for detailed API documentation
