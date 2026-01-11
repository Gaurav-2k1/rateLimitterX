@echo off
REM RateLimitX Debug Test for Windows
REM Save as test-rate-limit.bat

setlocal enabledelayedexpansion
set BASE_URL=http://localhost:8080/api/v1/debug

echo ==========================================
echo RateLimitX Debug Test Suite (Windows)
echo ==========================================

REM Generate timestamp for unique keys
set timestamp=%time:~0,2%%time:~3,2%%time:~6,2%

echo.
echo [CHECK] Testing server connection...
curl -s -o nul -w "HTTP Status: %%{http_code}" %BASE_URL%/sequential?requests=1^&maxRequests=10
echo.

echo.
echo [TEST 1] Sequential Requests
set key=test:seq:%timestamp%
echo Testing 5 requests with limit of 5...
curl -s -X POST "%BASE_URL%/sequential?key=%key%&requests=5&maxRequests=5&windowSeconds=60&algorithm=FIXED_WINDOW"
echo.

timeout /t 2 /nobreak > nul

echo.
echo [TEST 2] Single Request
set key=test:single:%timestamp%
echo Making a single request...
curl -s -X POST "%BASE_URL%/sequential?key=%key%&requests=1&maxRequests=10&windowSeconds=60&algorithm=FIXED_WINDOW"
echo.

timeout /t 2 /nobreak > nul

echo.
echo [TEST 3] Debug a Key
set key=test:debug:%timestamp%
echo Creating test data...
curl -s -X POST "%BASE_URL%/sequential?key=%key%&requests=2&maxRequests=10&windowSeconds=60&algorithm=FIXED_WINDOW" > nul
echo Checking key value...
curl -s -X GET "%BASE_URL%/key?key=%key%"
echo.

timeout /t 2 /nobreak > nul

echo.
echo [TEST 4] Simulate Requests
set key=test:sim:%timestamp%
echo Simulating 3 requests...
curl -s -X POST "%BASE_URL%/simulate?key=%key%&requests=3"
echo.

timeout /t 2 /nobreak > nul

echo.
echo [TEST 5] Concurrent Test
set key=test:concurrent:%timestamp%
echo Testing 20 concurrent requests with limit of 5...
curl -s -X POST "%BASE_URL%/concurrent?key=%key%&requests=20&maxRequests=5&windowSeconds=60&algorithm=FIXED_WINDOW&threads=10"
echo.

timeout /t 2 /nobreak > nul

echo.
echo [TEST 6] Compare Algorithms
echo Comparing all algorithms...
curl -s -X POST "%BASE_URL%/compare?requests=10&maxRequests=5&windowSeconds=60"
echo.

timeout /t 2 /nobreak > nul

echo.
echo [TEST 7] Health Check
curl -s http://localhost:8080/api/v1/health
echo.

echo.
echo ==========================================
echo Tests Complete!
echo ==========================================
echo.
echo Look for JSON responses above.
echo If you see errors, check application.properties has:
echo   spring.profiles.active=dev
echo.

pause