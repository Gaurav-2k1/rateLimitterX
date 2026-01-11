#!/bin/bash

# Simple RateLimitX Debug Test - No jq required
# Works on Windows Git Bash

BASE_URL="http://localhost:8080/api/v1/debug"

echo "=========================================="
echo "RateLimitX Debug Test Suite (Simple)"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if server is running
echo -e "\n${YELLOW}Checking if server is running...${NC}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/sequential?requests=1&maxRequests=10" 2>/dev/null)

if [ "$HTTP_CODE" = "000" ]; then
    echo -e "${RED}ERROR: Cannot connect to server at http://localhost:8080${NC}"
    echo "Please make sure your application is running with --spring.profiles.active=dev"
    exit 1
elif [ "$HTTP_CODE" = "404" ]; then
    echo -e "${RED}ERROR: Debug endpoints not found (404)${NC}"
    echo "Make sure:"
    echo "1. You added @Profile(\"dev\") to RateLimitDebugController"
    echo "2. You're running with: --spring.profiles.active=dev"
    exit 1
elif [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    echo -e "${RED}ERROR: Unauthorized (${HTTP_CODE})${NC}"
    echo "Debug endpoints should not require authentication"
    exit 1
else
    echo -e "${GREEN}✓ Server is running (HTTP ${HTTP_CODE})${NC}"
fi

# Test 1: Sequential Test
echo -e "\n${YELLOW}Test 1: Sequential Requests (Simple)${NC}"
KEY="test:seq:$(date +%s)"
echo "Testing 5 requests with limit of 5..."
RESPONSE=$(curl -s -X POST "$BASE_URL/sequential?key=$KEY&requests=5&maxRequests=5&windowSeconds=60&algorithm=FIXED_WINDOW")
echo "$RESPONSE"
echo ""

# Test 2: Raw JSON Output
echo -e "\n${YELLOW}Test 2: Single Request Test${NC}"
KEY="test:single:$(date +%s)"
echo "Making a single request..."
curl -s -X POST "$BASE_URL/sequential?key=$KEY&requests=1&maxRequests=10&windowSeconds=60&algorithm=FIXED_WINDOW"
echo -e "\n"

# Test 3: Multiple Single Requests
echo -e "\n${YELLOW}Test 3: Multiple Single Requests${NC}"
KEY="test:multi:$(date +%s)"
echo "Making 3 separate requests to the same key..."

for i in 1 2 3; do
    echo -e "\n--- Request $i ---"
    curl -s -X POST "$BASE_URL/sequential?key=$KEY&requests=1&maxRequests=5&windowSeconds=60&algorithm=FIXED_WINDOW"
    echo ""
done

# Test 4: Simulate Test
echo -e "\n${YELLOW}Test 4: Simulate Test (Raw Output)${NC}"
KEY="test:sim:$(date +%s)"
echo "Simulating 3 requests..."
curl -s -X POST "$BASE_URL/simulate?key=$KEY&requests=3"
echo -e "\n"

# Test 5: Debug Key
echo -e "\n${YELLOW}Test 5: Debug a Key${NC}"
KEY="test:debug:$(date +%s)"
echo "Creating some data..."
curl -s -X POST "$BASE_URL/sequential?key=$KEY&requests=2&maxRequests=10&windowSeconds=60&algorithm=FIXED_WINDOW" > /dev/null
echo "Checking key value..."
curl -s -X GET "$BASE_URL/key?key=$KEY"
echo -e "\n"

# Test 6: Health Check
echo -e "\n${YELLOW}Test 6: Application Health Check${NC}"
curl -s http://localhost:8080/api/v1/health
echo -e "\n"

echo -e "\n${GREEN}=========================================="
echo "Tests Complete!"
echo "==========================================${NC}"
echo ""
echo "If you see JSON responses above, the debug endpoints are working."
echo "If all responses show errors, check:"
echo "1. Application is running with profile: dev or test"
echo "2. RateLimitDebugController has @Profile annotation"
echo "3. No security blocking the /api/v1/debug/** endpoints"