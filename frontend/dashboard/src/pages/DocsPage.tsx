import { Link, useLocation } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Code,
  Book,
  Terminal,
  FileCode,
  CheckCircle2,
  AlertCircle,
  ArrowLeft,
  ExternalLink,
  Copy,
  Check,
} from 'lucide-react'
import { useState, useEffect } from 'react'

export default function DocsPage() {
  const [copiedCode, setCopiedCode] = useState<string | null>(null)
  const location = useLocation()

  // Handle hash navigation on mount and hash change
  useEffect(() => {
    const handleHashNavigation = () => {
      const hash = window.location.hash || location.hash
      if (hash) {
        const element = document.querySelector(hash)
        if (element) {
          const navHeight = 80 // Approximate height of sticky nav
          const elementPosition = element.getBoundingClientRect().top + window.pageYOffset
          const offsetPosition = elementPosition - navHeight

          window.scrollTo({
            top: offsetPosition,
            behavior: 'smooth',
          })
        }
      }
    }

    // Handle initial hash on mount
    const timer = setTimeout(handleHashNavigation, 100)
    
    // Listen for hash changes
    window.addEventListener('hashchange', handleHashNavigation)
    
    return () => {
      clearTimeout(timer)
      window.removeEventListener('hashchange', handleHashNavigation)
    }
  }, [location.hash])

  const handleAnchorClick = (e: React.MouseEvent<HTMLAnchorElement>, hash: string) => {
    e.preventDefault()
    const element = document.querySelector(hash)
    if (element) {
      const navHeight = 80
      const elementPosition = element.getBoundingClientRect().top + window.pageYOffset
      const offsetPosition = elementPosition - navHeight

      window.scrollTo({
        top: offsetPosition,
        behavior: 'smooth',
      })
      // Update URL without triggering scroll
      window.history.pushState(null, '', hash)
    }
  }

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text)
    setCopiedCode(id)
    setTimeout(() => setCopiedCode(null), 2000)
  }

  const CodeBlock = ({ code, language, id }: { code: string; language: string; id: string }) => (
    <div className="relative">
      <div className="absolute top-2 right-2">
        <Button
          variant="ghost"
          size="sm"
          className="h-8 w-8 p-0"
          onClick={() => copyToClipboard(code, id)}
        >
          {copiedCode === id ? (
            <Check className="h-4 w-4 text-green-600" />
          ) : (
            <Copy className="h-4 w-4" />
          )}
        </Button>
      </div>
      <pre className="bg-muted p-4 rounded-lg overflow-x-auto text-sm">
        <code>{code}</code>
      </pre>
    </div>
  )

  return (
    <div className="min-h-screen bg-background">
      {/* Navigation */}
      <nav className="border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 sticky top-0 z-50">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Book className="h-5 w-5 sm:h-6 sm:w-6 text-primary" />
            <span className="text-base sm:text-xl font-bold">Developer Documentation</span>
          </div>
          <div className="flex items-center gap-2 sm:gap-4">
            <Link to="/">
              <Button variant="ghost" size="sm" className="hidden sm:flex">
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Home
              </Button>
              <Button variant="ghost" size="sm" className="sm:hidden">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <Link to="/register">
              <Button size="sm" className="hidden sm:flex">Get Started</Button>
              <Button size="sm" className="sm:hidden">Start</Button>
            </Link>
          </div>
        </div>
      </nav>

      <div className="container mx-auto px-4 py-6 sm:py-12 max-w-5xl">
        {/* Header */}
        <div className="mb-8 sm:mb-12">
          <h1 className="text-2xl sm:text-3xl lg:text-4xl font-bold mb-4">RateLimitX Developer Guide</h1>
          <p className="text-base sm:text-lg lg:text-xl text-muted-foreground">
            Complete integration guide for developers. Learn how to integrate RateLimitX into your
            application using REST API or SDKs.
          </p>
        </div>

        {/* Quick Links */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 mb-8 sm:mb-12">
          <Card className="cursor-pointer hover:border-primary transition-colors">
            <a
              href="#api-integration"
              onClick={(e) => handleAnchorClick(e, '#api-integration')}
              className="block"
            >
              <CardHeader>
                <Terminal className="h-8 w-8 text-primary mb-2" />
                <CardTitle>REST API</CardTitle>
                <CardDescription>Direct HTTP integration</CardDescription>
              </CardHeader>
            </a>
          </Card>
          <Card className="cursor-pointer hover:border-primary transition-colors">
            <a
              href="#sdk-integration"
              onClick={(e) => handleAnchorClick(e, '#sdk-integration')}
              className="block"
            >
              <CardHeader>
                <Code className="h-8 w-8 text-primary mb-2" />
                <CardTitle>SDKs</CardTitle>
                <CardDescription>Language-specific libraries</CardDescription>
              </CardHeader>
            </a>
          </Card>
          <Card className="cursor-pointer hover:border-primary transition-colors">
            <a
              href="#examples"
              onClick={(e) => handleAnchorClick(e, '#examples')}
              className="block"
            >
              <CardHeader>
                <FileCode className="h-8 w-8 text-primary mb-2" />
                <CardTitle>Examples</CardTitle>
                <CardDescription>Code samples & tutorials</CardDescription>
              </CardHeader>
            </a>
          </Card>
        </div>

        {/* Getting Started */}
        <section id="getting-started" className="mb-12 sm:mb-16 scroll-mt-24">
          <h2 className="text-2xl sm:text-3xl font-bold mb-4 sm:mb-6">Getting Started</h2>
          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Step 1: Create an Account</CardTitle>
                <CardDescription>Sign up and get your API key</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-sm text-muted-foreground">
                  Register via the dashboard or API to get your unique API key. A default rate limit
                  rule (100 requests/minute) is automatically created for you.
                </p>
                <CodeBlock
                  id="register-api"
                  language="bash"
                  code={`curl -X POST https://api.ratelimitx.com/auth/register \\
  -H "Content-Type: application/json" \\
  -d '{
    "email": "your@email.com",
    "password": "your-password"
  }'`}
                />
                <div className="bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="h-5 w-5 text-blue-600 mt-0.5 flex-shrink-0" />
                    <div className="text-sm">
                      <p className="font-semibold text-blue-900 dark:text-blue-100 mb-1">
                        Save Your API Key
                      </p>
                      <p className="text-blue-800 dark:text-blue-200">
                        The response includes an <code className="bg-blue-100 dark:bg-blue-900 px-1 py-0.5 rounded">apiKey</code> field.
                        Save this securely - you'll need it for all API calls.
                      </p>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Step 2: Configure Rate Limit Rules</CardTitle>
                <CardDescription>Set up rules for your resources</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-sm text-muted-foreground mb-4">
                  Create rate limit rules via the dashboard or API. Each rule defines:
                </p>
                <ul className="list-disc list-inside space-y-2 text-sm text-muted-foreground ml-4 mb-4">
                  <li>
                    <strong>Resource:</strong> The endpoint or resource to protect (e.g.,
                    "api.payment.create")
                  </li>
                  <li>
                    <strong>Algorithm:</strong> TOKEN_BUCKET, SLIDING_WINDOW, or FIXED_WINDOW
                  </li>
                  <li>
                    <strong>Max Requests:</strong> Maximum number of requests allowed
                  </li>
                  <li>
                    <strong>Window:</strong> Time window in seconds
                  </li>
                  <li>
                    <strong>Identifier Type:</strong> USER_ID, IP_ADDRESS, API_KEY, or CUSTOM
                  </li>
                </ul>
                <CodeBlock
                  id="create-rule"
                  language="bash"
                  code={`curl -X POST https://api.ratelimitx.com/rules \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \\
  -d '{
    "resource": "api.payment.create",
    "algorithm": "TOKEN_BUCKET",
    "maxRequests": 100,
    "windowSeconds": 60,
    "identifierType": "USER_ID"
  }'`}
                />
              </CardContent>
            </Card>
          </div>
        </section>

        {/* API Integration */}
        <section id="api-integration" className="mb-12 sm:mb-16 scroll-mt-24">
          <h2 className="text-2xl sm:text-3xl font-bold mb-4 sm:mb-6">REST API Integration</h2>
          <p className="text-muted-foreground mb-6">
            RateLimitX provides a simple REST API that works with any programming language. Make
            HTTP requests to check rate limits before processing requests.
          </p>

          <Card className="mb-6">
            <CardHeader>
              <CardTitle>Check Rate Limit</CardTitle>
              <CardDescription>Endpoint: POST /api/v1/check</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <h4 className="font-semibold mb-2">Request</h4>
                <CodeBlock
                  id="check-request"
                  language="bash"
                  code={`curl -X POST https://api.ratelimitx.com/api/v1/check \\
  -H "Content-Type: application/json" \\
  -H "X-API-Key: YOUR_API_KEY" \\
  -d '{
    "identifier": "user123",
    "resource": "api.payment.create",
    "tokens": 1
  }'`}
                />
              </div>
              <div>
                <h4 className="font-semibold mb-2">Request Parameters</h4>
                <div className="bg-muted p-4 rounded-lg">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b">
                        <th className="text-left p-2">Parameter</th>
                        <th className="text-left p-2">Type</th>
                        <th className="text-left p-2">Required</th>
                        <th className="text-left p-2">Description</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr className="border-b">
                        <td className="p-2 font-mono">identifier</td>
                        <td className="p-2">string</td>
                        <td className="p-2">Yes</td>
                        <td className="p-2">User ID, IP address, or custom identifier</td>
                      </tr>
                      <tr className="border-b">
                        <td className="p-2 font-mono">resource</td>
                        <td className="p-2">string</td>
                        <td className="p-2">Yes</td>
                        <td className="p-2">Resource/endpoint being accessed</td>
                      </tr>
                      <tr>
                        <td className="p-2 font-mono">tokens</td>
                        <td className="p-2">number</td>
                        <td className="p-2">No</td>
                        <td className="p-2">Number of tokens to consume (default: 1)</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
              <div>
                <h4 className="font-semibold mb-2">Response</h4>
                <CodeBlock
                  id="check-response"
                  language="json"
                  code={`{
  "allowed": true,
  "remaining": 99,
  "resetAt": 1704067200000,
  "retryAfter": 0
}`}
                />
              </div>
              <div>
                <h4 className="font-semibold mb-2">Response Fields</h4>
                <div className="bg-muted p-4 rounded-lg">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b">
                        <th className="text-left p-2">Field</th>
                        <th className="text-left p-2">Type</th>
                        <th className="text-left p-2">Description</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr className="border-b">
                        <td className="p-2 font-mono">allowed</td>
                        <td className="p-2">boolean</td>
                        <td className="p-2">Whether the request is allowed</td>
                      </tr>
                      <tr className="border-b">
                        <td className="p-2 font-mono">remaining</td>
                        <td className="p-2">number</td>
                        <td className="p-2">Remaining tokens/requests in the window</td>
                      </tr>
                      <tr className="border-b">
                        <td className="p-2 font-mono">resetAt</td>
                        <td className="p-2">number</td>
                        <td className="p-2">Unix timestamp when the limit resets</td>
                      </tr>
                      <tr>
                        <td className="p-2 font-mono">retryAfter</td>
                        <td className="p-2">number</td>
                        <td className="p-2">Seconds to wait before retrying (0 if allowed)</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Error Responses</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <h4 className="font-semibold mb-2">429 Too Many Requests</h4>
                <CodeBlock
                  id="error-429"
                  language="json"
                  code={`{
  "error": "Rate limit exceeded",
  "retryAfter": 45
}`}
                />
              </div>
              <div>
                <h4 className="font-semibold mb-2">401 Unauthorized</h4>
                <CodeBlock
                  id="error-401"
                  language="json"
                  code={`{
  "error": "Invalid or missing API key"
}`}
                />
              </div>
            </CardContent>
          </Card>
        </section>

        {/* SDK Integration */}
        <section id="sdk-integration" className="mb-12 sm:mb-16 scroll-mt-24">
          <h2 className="text-2xl sm:text-3xl font-bold mb-4 sm:mb-6">SDK Integration</h2>
          <p className="text-muted-foreground mb-6">
            Official SDKs provide a more convenient way to integrate RateLimitX into your
            application. SDKs handle retries, caching, and error handling automatically.
          </p>

          <div className="bg-yellow-50 dark:bg-yellow-950 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4 mb-6">
            <div className="flex items-start gap-2">
              <AlertCircle className="h-5 w-5 text-yellow-600 mt-0.5 flex-shrink-0" />
              <div className="text-sm">
                <p className="font-semibold text-yellow-900 dark:text-yellow-100 mb-1">
                  SDKs Coming Soon
                </p>
                <p className="text-yellow-800 dark:text-yellow-200">
                  Official SDKs for Java, Node.js, Python, and Go are in development. For now, use
                  the REST API directly. The examples below show how SDKs will work once released.
                </p>
              </div>
            </div>
          </div>

          <div className="space-y-6">
            {/* Node.js SDK */}
            <Card>
              <CardHeader>
                <CardTitle>Node.js / Express SDK</CardTitle>
                <CardDescription>npm install @ratelimitx/sdk</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <CodeBlock
                  id="nodejs-sdk"
                  language="javascript"
                  code={`const { RateLimitX } = require('@ratelimitx/sdk');

const rateLimiter = new RateLimitX({
  apiKey: process.env.RATELIMITX_API_KEY,
  baseUrl: 'https://api.ratelimitx.com'
});

// Express middleware
app.use(async (req, res, next) => {
  const result = await rateLimiter.check({
    identifier: req.user?.id || req.ip,
    resource: req.path,
    tokens: 1
  });

  if (!result.allowed) {
    return res.status(429).json({
      error: 'Rate limit exceeded',
      retryAfter: result.retryAfter
    });
  }

  res.setHeader('X-RateLimit-Remaining', result.remaining);
  res.setHeader('X-RateLimit-Reset', result.resetAt);
  next();
});`}
                />
              </CardContent>
            </Card>

            {/* Python SDK */}
            <Card>
              <CardHeader>
                <CardTitle>Python / Flask SDK</CardTitle>
                <CardDescription>pip install ratelimitx</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <CodeBlock
                  id="python-sdk"
                  language="python"
                  code={`from ratelimitx import RateLimitX
from flask import request, jsonify

rate_limiter = RateLimitX(
    api_key=os.getenv('RATELIMITX_API_KEY'),
    base_url='https://api.ratelimitx.com'
)

@app.before_request
def check_rate_limit():
    identifier = request.remote_addr
    if hasattr(request, 'user'):
        identifier = request.user.id
    
    result = rate_limiter.check(
        identifier=identifier,
        resource=request.path,
        tokens=1
    )
    
    if not result.allowed:
        return jsonify({
            'error': 'Rate limit exceeded',
            'retry_after': result.retry_after
        }), 429
    
    response = make_response()
    response.headers['X-RateLimit-Remaining'] = result.remaining
    response.headers['X-RateLimit-Reset'] = result.reset_at
    return response`}
                />
              </CardContent>
            </Card>

            {/* Java SDK */}
            <Card>
              <CardHeader>
                <CardTitle>Java / Spring Boot SDK</CardTitle>
                <CardDescription>Maven: com.ratelimitx:ratelimitx-java</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <CodeBlock
                  id="java-sdk"
                  language="java"
                  code={`import com.ratelimitx.RateLimitX;
import com.ratelimitx.annotation.RateLimit;

@RestController
public class PaymentController {
    
    @Autowired
    private RateLimitX rateLimitX;
    
    @PostMapping("/payment/create")
    @RateLimit(resource = "api.payment.create", tokens = 1)
    public ResponseEntity<?> createPayment(@RequestParam String userId) {
        // Your business logic here
        return ResponseEntity.ok().build();
    }
}

// Configuration
@Configuration
public class RateLimitConfig {
    @Bean
    public RateLimitX rateLimitX() {
        return RateLimitX.builder()
            .apiKey(System.getenv("RATELIMITX_API_KEY"))
            .baseUrl("https://api.ratelimitx.com")
            .build();
    }
}`}
                />
              </CardContent>
            </Card>

            {/* Go SDK */}
            <Card>
              <CardHeader>
                <CardTitle>Go SDK</CardTitle>
                <CardDescription>go get github.com/ratelimitx/go-sdk</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <CodeBlock
                  id="go-sdk"
                  language="go"
                  code={`package main

import (
    "github.com/ratelimitx/go-sdk"
    "net/http"
)

func rateLimitMiddleware(next http.Handler) http.Handler {
    client := ratelimitx.NewClient(
        os.Getenv("RATELIMITX_API_KEY"),
        "https://api.ratelimitx.com",
    )
    
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        identifier := r.RemoteAddr
        if userID := r.Header.Get("X-User-ID"); userID != "" {
            identifier = userID
        }
        
        result, err := client.Check(r.Context(), ratelimitx.CheckRequest{
            Identifier: identifier,
            Resource:  r.URL.Path,
            Tokens:    1,
        })
        
        if err != nil || !result.Allowed {
            http.Error(w, "Rate limit exceeded", http.StatusTooManyRequests)
            return
        }
        
        w.Header().Set("X-RateLimit-Remaining", strconv.Itoa(result.Remaining))
        w.Header().Set("X-RateLimit-Reset", strconv.FormatInt(result.ResetAt, 10))
        next.ServeHTTP(w, r)
    })
}`}
                />
              </CardContent>
            </Card>
          </div>
        </section>

        {/* Code Examples */}
        <section id="examples" className="mb-12 sm:mb-16 scroll-mt-24">
          <h2 className="text-2xl sm:text-3xl font-bold mb-4 sm:mb-6">Code Examples</h2>
          <p className="text-muted-foreground mb-6">
            Complete integration examples for popular frameworks and languages.
          </p>

          <div className="space-y-6">
            {/* Node.js/Express Example */}
            <Card>
              <CardHeader>
                <CardTitle>Node.js / Express Example</CardTitle>
              </CardHeader>
              <CardContent>
                <CodeBlock
                  id="nodejs-example"
                  language="javascript"
                  code={`const express = require('express');
const axios = require('axios');

const app = express();
const RATELIMITX_API_KEY = process.env.RATELIMITX_API_KEY;
const RATELIMITX_BASE_URL = 'https://api.ratelimitx.com';

async function checkRateLimit(req, res, next) {
  try {
    const identifier = req.user?.id || req.ip;
    const resource = req.path;
    
    const response = await axios.post(
      \`\${RATELIMITX_BASE_URL}/api/v1/check\`,
      {
        identifier,
        resource,
        tokens: 1
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'X-API-Key': RATELIMITX_API_KEY
        }
      }
    );
    
    const { allowed, remaining, resetAt, retryAfter } = response.data;
    
    if (!allowed) {
      return res.status(429).json({
        error: 'Rate limit exceeded',
        retryAfter
      });
    }
    
    // Set rate limit headers
    res.setHeader('X-RateLimit-Remaining', remaining);
    res.setHeader('X-RateLimit-Reset', resetAt);
    
    next();
  } catch (error) {
    // Fail open - allow request if rate limit service is unavailable
    console.error('Rate limit check failed:', error);
    next();
  }
}

app.use(checkRateLimit);

app.post('/payment/create', (req, res) => {
  // Your payment processing logic
  res.json({ success: true });
});

app.listen(3000);`}
                />
              </CardContent>
            </Card>

            {/* Python/Flask Example */}
            <Card>
              <CardHeader>
                <CardTitle>Python / Flask Example</CardTitle>
              </CardHeader>
              <CardContent>
                <CodeBlock
                  id="python-example"
                  language="python"
                  code={`from flask import Flask, request, jsonify, make_response
import requests
import os

app = Flask(__name__)
RATELIMITX_API_KEY = os.getenv('RATELIMITX_API_KEY')
RATELIMITX_BASE_URL = 'https://api.ratelimitx.com'

def check_rate_limit():
    identifier = request.remote_addr
    if hasattr(request, 'user'):
        identifier = request.user.id
    
    try:
        response = requests.post(
            f'{RATELIMITX_BASE_URL}/api/v1/check',
            json={
                'identifier': identifier,
                'resource': request.path,
                'tokens': 1
            },
            headers={
                'Content-Type': 'application/json',
                'X-API-Key': RATELIMITX_API_KEY
            }
        )
        
        data = response.json()
        
        if not data['allowed']:
            return jsonify({
                'error': 'Rate limit exceeded',
                'retry_after': data['retryAfter']
            }), 429
        
        # Set rate limit headers
        response_obj = make_response()
        response_obj.headers['X-RateLimit-Remaining'] = data['remaining']
        response_obj.headers['X-RateLimit-Reset'] = data['resetAt']
        return response_obj
        
    except Exception as e:
        # Fail open - allow request if rate limit service is unavailable
        print(f'Rate limit check failed: {e}')
        return None

@app.before_request
def before_request():
    result = check_rate_limit()
    if result:
        return result

@app.route('/payment/create', methods=['POST'])
def create_payment():
    # Your payment processing logic
    return jsonify({'success': True})

if __name__ == '__main__':
    app.run(port=5000)`}
                />
              </CardContent>
            </Card>

            {/* Java/Spring Boot Example */}
            <Card>
              <CardHeader>
                <CardTitle>Java / Spring Boot Example</CardTitle>
              </CardHeader>
              <CardContent>
                <CodeBlock
                  id="java-example"
                  language="java"
                  code={`package com.example.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
public class PaymentController {
    
    @PostMapping("/payment/create")
    public ResponseEntity<?> createPayment(@RequestParam String userId) {
        // Your payment processing logic
        return ResponseEntity.ok().build();
    }
}

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    @Value("\${ratelimitx.api.key}")
    private String apiKey;
    
    @Value("\${ratelimitx.api.url:https://api.ratelimitx.com}")
    private String baseUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws Exception {
        String identifier = request.getRemoteAddr();
        if (request.getHeader("X-User-ID") != null) {
            identifier = request.getHeader("X-User-ID");
        }
        
        try {
            String url = baseUrl + "/api/v1/check";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);
            
            Map<String, Object> body = Map.of(
                "identifier", identifier,
                "resource", request.getRequestURI(),
                "tokens", 1
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> result = restTemplate.postForEntity(url, entity, Map.class);
            
            Map<String, Object> data = result.getBody();
            if (data != null && !(Boolean) data.get("allowed")) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
                return;
            }
            
            if (data != null) {
                response.setHeader("X-RateLimit-Remaining", 
                    String.valueOf(data.get("remaining")));
                response.setHeader("X-RateLimit-Reset", 
                    String.valueOf(data.get("resetAt")));
            }
        } catch (Exception e) {
            // Fail open - allow request if rate limit service is unavailable
            logger.error("Rate limit check failed", e);
        }
        
        filterChain.doFilter(request, response);
    }
}`}
                />
              </CardContent>
            </Card>
          </div>
        </section>

        {/* Best Practices */}
        <section id="best-practices" className="mb-12 sm:mb-16 scroll-mt-24">
          <h2 className="text-2xl sm:text-3xl font-bold mb-4 sm:mb-6">Best Practices</h2>
          <div className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>1. Fail-Open Strategy</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  Always implement a fail-open strategy. If the rate limit service is unavailable,
                  allow requests to proceed rather than blocking all traffic.
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>2. Set Rate Limit Headers</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  Include rate limit information in your API responses using standard headers:
                  <code className="bg-muted px-1 py-0.5 rounded mx-1">X-RateLimit-Remaining</code>
                  and
                  <code className="bg-muted px-1 py-0.5 rounded mx-1">X-RateLimit-Reset</code>
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>3. Use Appropriate Identifiers</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  Choose the right identifier type for your use case:
                  <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li>
                      <strong>USER_ID:</strong> For authenticated users
                    </li>
                    <li>
                      <strong>IP_ADDRESS:</strong> For anonymous users
                    </li>
                    <li>
                      <strong>API_KEY:</strong> For API key-based rate limiting
                    </li>
                    <li>
                      <strong>CUSTOM:</strong> For custom identifiers
                    </li>
                  </ul>
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>4. Handle 429 Responses Gracefully</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  When a rate limit is exceeded, return a 429 status code with a{' '}
                  <code className="bg-muted px-1 py-0.5 rounded">Retry-After</code> header
                  indicating when the client can retry.
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>5. Cache Rate Limit Results</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  Consider caching rate limit results locally for a short period (1-5 seconds) to
                  reduce API calls while maintaining accuracy.
                </p>
              </CardContent>
            </Card>
          </div>
        </section>

        {/* API Reference */}
        <section id="api-reference" className="mb-12 sm:mb-16 scroll-mt-24">
          <h2 className="text-2xl sm:text-3xl font-bold mb-4 sm:mb-6">API Reference</h2>
          <Card>
            <CardHeader>
              <CardTitle>Base URL</CardTitle>
            </CardHeader>
            <CardContent>
              <code className="bg-muted px-2 py-1 rounded">
                https://api.ratelimitx.com
              </code>
            </CardContent>
          </Card>

          <div className="mt-6 space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>POST /api/v1/check</CardTitle>
                <CardDescription>Check if a request is allowed</CardDescription>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground mb-2">Headers:</p>
                <ul className="list-disc list-inside ml-4 text-sm text-muted-foreground mb-4">
                  <li>
                    <code>X-API-Key:</code> Your API key (required)
                  </li>
                  <li>
                    <code>Content-Type:</code> application/json
                  </li>
                </ul>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>POST /auth/register</CardTitle>
                <CardDescription>Register a new account</CardDescription>
              </CardHeader>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>POST /auth/login</CardTitle>
                <CardDescription>Login and get JWT token</CardDescription>
              </CardHeader>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>GET /rules</CardTitle>
                <CardDescription>List all rate limit rules</CardDescription>
              </CardHeader>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>POST /rules</CardTitle>
                <CardDescription>Create a new rate limit rule</CardDescription>
              </CardHeader>
            </Card>
          </div>
        </section>

        {/* CTA */}
        <section className="bg-primary/5 rounded-lg p-6 sm:p-8 text-center">
          <h2 className="text-xl sm:text-2xl font-bold mb-4">Ready to Get Started?</h2>
          <p className="text-muted-foreground mb-6">
            Sign up for free and start protecting your APIs today
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link to="/register">
              <Button size="lg">Create Free Account</Button>
            </Link>
            <Link to="/dashboard">
              <Button size="lg" variant="outline">
                View Dashboard
              </Button>
            </Link>
          </div>
        </section>
      </div>
    </div>
  )
}

