// Complete Test Script for RateLimitDebugController
// Run with: node test-complete.js
// Make sure your app is running with --spring.profiles.active=dev

const BASE_URL = 'http://localhost:8080/api/v1/debug';

const colors = {
  reset: '\x1b[0m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m'
};

function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

function separator() {
  log('='.repeat(70), 'blue');
}

async function httpRequest(url, method = 'GET') {
  try {
    const response = await fetch(url, { method });
    const text = await response.text();
    
    if (!text || text.trim() === '') {
      return { 
        success: false, 
        status: response.status,
        error: 'Empty response from server'
      };
    }
    
    try {
      const data = JSON.parse(text);
      return { success: response.ok, status: response.status, data };
    } catch (e) {
      return { 
        success: false, 
        status: response.status,
        error: `Invalid JSON: ${text.substring(0, 100)}...`
      };
    }
  } catch (error) {
    return { 
      success: false, 
      error: error.message,
      connectionError: true
    };
  }
}

async function checkServerHealth() {
  separator();
  log('🏥 Checking Server Health...', 'yellow');
  separator();
  
  const result = await httpRequest('http://localhost:8080/api/v1/health');
  
  if (result.connectionError) {
    log(`✗ Cannot connect to server: ${result.error}`, 'red');
    log('\nMake sure:', 'yellow');
    log('  1. Your Spring Boot application is running', 'yellow');
    log('  2. It\'s running on port 8080', 'yellow');
    log('  3. Started with: --spring.profiles.active=dev', 'yellow');
    return false;
  }
  
  if (!result.success) {
    log(`✗ Health check failed (${result.status}): ${result.error}`, 'red');
    return false;
  }
  
  log(`✓ Server is healthy (HTTP ${result.status})`, 'green');
  log(`Response: ${JSON.stringify(result.data, null, 2)}`, 'cyan');
  return true;
}

async function checkDebugEndpoint() {
  separator();
  log('🔍 Checking Debug Endpoints...', 'yellow');
  separator();
  
  // Try the key endpoint
  const result = await httpRequest(`${BASE_URL}/key?key=test:check`);
  
  if (result.connectionError) {
    log(`✗ Connection error: ${result.error}`, 'red');
    return false;
  }
  
  if (result.status === 404) {
    log('✗ Debug endpoints not found (404)', 'red');
    log('\nPossible issues:', 'yellow');
    log('  1. Application not running with "dev" or "test" profile', 'yellow');
    log('  2. @Profile annotation blocking the controller', 'yellow');
    log('\nCheck application logs for:', 'yellow');
    log('  "The following profiles are active: dev"', 'cyan');
    return false;
  }
  
  if (!result.success) {
    log(`✗ Debug endpoint error (${result.status}): ${result.error}`, 'red');
    return false;
  }
  
  log('✓ Debug endpoints are accessible', 'green');
  return true;
}

async function testSequential() {
  separator();
  log('📊 Test 1: Sequential Requests', 'yellow');
  separator();
  log('Testing 10 requests with limit of 5...', 'cyan');
  
  const key = `test:seq:${Date.now()}`;
  const url = `${BASE_URL}/sequential?key=${key}&requests=10&maxRequests=5&windowSeconds=60&algorithm=FIXED_WINDOW`;
  
  const result = await httpRequest(url, 'POST');
  
  if (!result.success) {
    log(`✗ Request failed: ${result.error || result.status}`, 'red');
    return { passed: false };
  }
  
  const data = result.data?.data || result.data;
  const { allowedCount, deniedCount, success, message, requests } = data;
  
  log('\nResults:', 'blue');
  log(`  Allowed: ${allowedCount} (expected: 5)`, allowedCount === 5 ? 'green' : 'red');
  log(`  Denied: ${deniedCount} (expected: 5)`, deniedCount === 5 ? 'green' : 'red');
  log(`  Success: ${success}`, success ? 'green' : 'red');
  log(`  Message: ${message}`, 'cyan');
  
  if (requests && requests.length > 0) {
    log('\nFirst 5 requests:', 'blue');
    requests.slice(0, 5).forEach(req => {
      const status = req.allowed ? '✓ ALLOWED' : '✗ DENIED';
      const color = req.allowed ? 'green' : 'red';
      log(`  Request #${req.requestNumber}: ${status} (Remaining: ${req.remaining})`, color);
    });
  }
  
  if (success) {
    log('\n✓ Sequential test PASSED', 'green');
    return { passed: true };
  } else {
    log('\n✗ Sequential test FAILED', 'red');
    return { passed: false };
  }
}

async function testConcurrent() {
  separator();
  log('⚡ Test 2: Concurrent Requests (Race Detection)', 'yellow');
  separator();
  log('Testing 50 concurrent requests with limit of 10...', 'cyan');
  
  const key = `test:concurrent:${Date.now()}`;
  const url = `${BASE_URL}/concurrent?key=${key}&requests=50&maxRequests=10&windowSeconds=60&algorithm=FIXED_WINDOW&threads=20`;
  
  const result = await httpRequest(url, 'POST');
  
  if (!result.success) {
    log(`✗ Request failed: ${result.error || result.status}`, 'red');
    return { passed: false };
  }
  
  const data = result.data?.data || result.data;
  const { allowedCount, deniedCount, hasRaceCondition, durationMs, message } = data;
  
  log('\nResults:', 'blue');
  log(`  Allowed: ${allowedCount} (expected: ~10)`, Math.abs(allowedCount - 10) <= 2 ? 'green' : 'red');
  log(`  Denied: ${deniedCount}`, 'cyan');
  log(`  Duration: ${durationMs}ms`, 'cyan');
  log(`  Race Condition: ${hasRaceCondition ? 'YES ⚠️' : 'NO ✓'}`, hasRaceCondition ? 'red' : 'green');
  log(`  Message: ${message}`, 'cyan');
  
  if (!hasRaceCondition) {
    log('\n✓ Concurrent test PASSED - No race condition', 'green');
    return { passed: true };
  } else {
    log('\n✗ Concurrent test FAILED - Race condition detected!', 'red');
    log('  Multiple requests bypassed the limit', 'yellow');
    log('  This means the rate limiting is not atomic', 'yellow');
    return { passed: false };
  }
}

async function testSimulate() {
  separator();
  log('🔍 Test 3: Simulate Requests (Count Verification)', 'yellow');
  separator();
  log('Simulating 5 requests to verify count increments...', 'cyan');
  
  const key = `test:sim:${Date.now()}`;
  const url = `${BASE_URL}/simulate?key=${key}&requests=5`;
  
  const result = await httpRequest(url, 'POST');
  
  if (!result.success) {
    log(`✗ Request failed: ${result.error || result.status}`, 'red');
    return { passed: false };
  }
  
  const data = result.data?.data || result.data;
  const { anomalyCount, errorCount, traces } = data;
  
  log('\nResults:', 'blue');
  log(`  Anomalies: ${anomalyCount}`, anomalyCount === 0 ? 'green' : 'red');
  log(`  Errors: ${errorCount}`, errorCount === 0 ? 'green' : 'red');
  
  if (traces && traces.length > 0) {
    log('\nCount Progression:', 'blue');
    traces.forEach(trace => {
      const status = trace.correct ? '✓' : '✗';
      const color = trace.correct ? 'green' : 'red';
      log(`  Request #${trace.requestNum}: ${status} Before=${trace.beforeCount}, After=${trace.afterCount}, Expected=${trace.expected}`, color);
    });
  }
  
  if (anomalyCount === 0 && errorCount === 0) {
    log('\n✓ Simulation test PASSED - All counts correct', 'green');
    return { passed: true };
  } else {
    log('\n✗ Simulation test FAILED - Count anomalies detected', 'red');
    return { passed: false };
  }
}

async function testCompare() {
  separator();
  log('🔬 Test 4: Compare All Algorithms', 'yellow');
  separator();
  log('Comparing Fixed Window, Sliding Window, and Token Bucket...', 'cyan');
  
  const url = `${BASE_URL}/compare?requests=10&maxRequests=5&windowSeconds=60`;
  
  const result = await httpRequest(url, 'POST');
  
  if (!result.success) {
    log(`✗ Request failed: ${result.error || result.status}`, 'red');
    return { passed: false };
  }
  
  const data = result.data?.data || result.data;
  let allPassed = true;
  
  log('\nResults by Algorithm:', 'blue');
  
  const algos = ['FIXED_WINDOW', 'SLIDING_WINDOW', 'TOKEN_BUCKET'];
  algos.forEach(algo => {
    if (data[algo]) {
      const algoData = data[algo];
      const passed = algoData.allowedCount === 5;
      if (!passed) allPassed = false;
      
      log(`\n  ${algo}:`, 'yellow');
      log(`    Allowed: ${algoData.allowedCount}/5`, passed ? 'green' : 'red');
      log(`    Denied: ${algoData.deniedCount}`, 'cyan');
      log(`    Success: ${algoData.success ? '✓' : '✗'}`, passed ? 'green' : 'red');
      log(`    Message: ${algoData.message}`, 'cyan');
    }
  });
  
  if (allPassed) {
    log('\n✓ Algorithm comparison PASSED', 'green');
    return { passed: true };
  } else {
    log('\n✗ Algorithm comparison FAILED - Some algorithms failed', 'red');
    return { passed: false };
  }
}

async function testDebugKey() {
  separator();
  log('🔑 Test 5: Debug Redis Key', 'yellow');
  separator();
  
  const key = `test:debug:${Date.now()}`;
  
  log('Creating test data...', 'cyan');
  await httpRequest(`${BASE_URL}/sequential?key=${key}&requests=3&maxRequests=10&windowSeconds=60&algorithm=FIXED_WINDOW`, 'POST');
  
  log(`Debugging key: ${key}`, 'cyan');
  const result = await httpRequest(`${BASE_URL}/key?key=${key}`);
  
  if (!result.success) {
    log(`✗ Request failed: ${result.error || result.status}`, 'red');
    return { passed: false };
  }
  
  const data = result.data?.data || result.data;
  
  log('\nKey Information:', 'blue');
  log(`  Key: ${data.key}`, 'cyan');
  log(`  Exists: ${data.exists}`, data.exists ? 'green' : 'red');
  log(`  Value: ${data.value}`, 'cyan');
  log(`  Numeric Value: ${data.numericValue}`, 'cyan');
  
  if (data.hashFields && data.hashFields.length > 0) {
    log(`  Hash Fields: ${data.hashFields.join(', ')}`, 'cyan');
  }
  
  if (data.sortedSetCardinality) {
    log(`  Sorted Set Size: ${data.sortedSetCardinality}`, 'cyan');
  }
  
  if (data.exists) {
    log('\n✓ Debug key test PASSED', 'green');
    return { passed: true };
  } else {
    log('\n⚠️  Key does not exist (might be normal)', 'yellow');
    return { passed: true };
  }
}

async function testRapidFire() {
  separator();
  log('🔥 Test 6: Rapid Fire', 'yellow');
  separator();
  log('Sending 15 rapid sequential requests (limit: 10)...', 'cyan');
  
  const key = `test:rapid:${Date.now()}`;
  let allowed = 0, denied = 0;
  
  for (let i = 1; i <= 15; i++) {
    const url = `${BASE_URL}/sequential?key=${key}&requests=1&maxRequests=10&windowSeconds=60&algorithm=FIXED_WINDOW`;
    const result = await httpRequest(url, 'POST');
    
    if (result.success) {
      const data = result.data?.data || result.data;
      if (data.requests && data.requests[0]) {
        const isAllowed = data.requests[0].allowed;
        const remaining = data.requests[0].remaining;
        
        if (isAllowed) {
          allowed++;
          log(`  Request ${i}: ✓ ALLOWED (Remaining: ${remaining})`, 'green');
        } else {
          denied++;
          log(`  Request ${i}: ✗ DENIED (Remaining: ${remaining})`, 'red');
        }
      }
    } else {
      log(`  Request ${i}: ERROR - ${result.error}`, 'red');
    }
    
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  
  log('\nRapid Fire Results:', 'blue');
  log(`  Allowed: ${allowed}/15 (expected: 10)`, allowed === 10 ? 'green' : 'red');
  log(`  Denied: ${denied}/15 (expected: 5)`, denied === 5 ? 'green' : 'red');
  
  if (allowed === 10 && denied === 5) {
    log('\n✓ Rapid fire test PASSED', 'green');
    return { passed: true };
  } else {
    log('\n✗ Rapid fire test FAILED', 'red');
    if (allowed > 10) {
      log(`  Too many allowed (${allowed}). Race condition likely.`, 'yellow');
    } else if (allowed < 10) {
      log(`  Too few allowed (${allowed}). Check Redis connection.`, 'yellow');
    }
    return { passed: false };
  }
}

async function runAllTests() {
  console.clear();
  separator();
  log('🚀 RateLimitX Complete Test Suite', 'magenta');
  separator();
  
  const stats = {
    total: 0,
    passed: 0,
    failed: 0
  };
  
  // Check server first
  const serverOk = await checkServerHealth();
  if (!serverOk) {
    log('\n❌ Cannot proceed - server is not reachable', 'red');
    process.exit(1);
  }
  
  await new Promise(r => setTimeout(r, 1000));
  
  // Check debug endpoints
  const debugOk = await checkDebugEndpoint();
  if (!debugOk) {
    log('\n❌ Cannot proceed - debug endpoints not available', 'red');
    log('\nTo fix this:', 'yellow');
    log('  1. Add to application.properties: spring.profiles.active=dev', 'cyan');
    log('  2. Or start with: java -jar app.jar --spring.profiles.active=dev', 'cyan');
    log('  3. Restart your application', 'cyan');
    process.exit(1);
  }
  
  await new Promise(r => setTimeout(r, 1000));
  
  // Run all tests
  const tests = [
    { name: 'Sequential', fn: testSequential },
    { name: 'Concurrent', fn: testConcurrent },
    { name: 'Simulate', fn: testSimulate },
    { name: 'Compare', fn: testCompare },
    { name: 'Debug Key', fn: testDebugKey },
    { name: 'Rapid Fire', fn: testRapidFire }
  ];
  
  for (const test of tests) {
    stats.total++;
    const result = await test.fn();
    if (result.passed) {
      stats.passed++;
    } else {
      stats.failed++;
    }
    await new Promise(r => setTimeout(r, 1500));
  }
  
  // Summary
  separator();
  log('📊 Test Summary', 'magenta');
  separator();
  log(`Total Tests: ${stats.total}`, 'cyan');
  log(`Passed: ${stats.passed}`, 'green');
  log(`Failed: ${stats.failed}`, stats.failed === 0 ? 'green' : 'red');
  
  if (stats.failed === 0) {
    separator();
    log('🎉 ALL TESTS PASSED! Rate limiting is working correctly!', 'green');
    separator();
  } else {
    separator();
    log('⚠️  SOME TESTS FAILED - Review the failures above', 'yellow');
    separator();
    
    log('\nCommon Issues:', 'yellow');
    log('  • Race condition detected → Use Lua script-based algorithms', 'cyan');
    log('  • Count anomalies → Check Redis atomicity', 'cyan');
    log('  • Connection errors → Verify Upstash Redis credentials', 'cyan');
  }
}

// Run tests
runAllTests().catch(error => {
  log(`\n❌ Test suite failed: ${error.message}`, 'red');
  console.error(error);
  process.exit(1);
});