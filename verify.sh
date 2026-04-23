#!/bin/bash

echo "========================================="
echo "Optimal Truck Load Planner - Verification"
echo "========================================="
echo ""

echo "1. Checking Java version..."
java -version 2>&1 | head -n 1
echo ""

echo "2. Running tests..."
./gradlew test --console=plain 2>&1 | tail -n 5
echo ""

echo "3. Checking code coverage..."
./gradlew jacocoTestCoverageVerification --console=plain 2>&1 | grep -E "(BUILD|coverage)"
echo ""

echo "4. Building application..."
./gradlew clean build -x test --console=plain 2>&1 | tail -n 3
echo ""

echo "5. Verifying JAR file..."
if [ -f "build/libs/optimal-truck-load-planner-1.0.0.jar" ]; then
    echo "✅ JAR file created successfully"
    ls -lh build/libs/*.jar
else
    echo "❌ JAR file not found"
fi
echo ""

echo "6. Checking Docker configuration..."
if [ -f "Dockerfile" ] && [ -f "docker-compose.yml" ]; then
    echo "✅ Docker files present"
else
    echo "❌ Docker files missing"
fi
echo ""

echo "7. Verifying documentation..."
for file in README.md QUICKSTART.md sample-request.json; do
    if [ -f "$file" ]; then
        echo "✅ $file exists"
    else
        echo "❌ $file missing"
    fi
done
echo ""

echo "========================================="
echo "Verification Complete!"
echo "========================================="
echo ""
echo "Next steps:"
echo "1. Test locally: ./gradlew bootRun"
echo "2. Test with Docker: docker compose up --build"
echo "3. Push to GitHub (see GITHUB_SETUP.md)"
echo ""
