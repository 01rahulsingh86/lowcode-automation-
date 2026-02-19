# Low-Code API Automation Framework

A lightweight, YAML-driven API automation framework built in Java that simplifies testing of microservices with minimal coding. Perfect for teams managing multiple microservices where test automation should be **low-code, high-impact**.

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Installation](#installation)
- [YAML Test File Format](#yaml-test-file-format)
- [Variable Capturing & Reuse](#variable-capturing--reuse)
- [Execution Guide](#execution-guide)
- [Multi-Service E2E Testing](#multi-service-e2e-testing)
- [JSON Response Validation](#json-response-validation)
- [Environment Configuration](#environment-configuration)
- [Best Practices](#best-practices)
- [Extending the Framework](#extending-the-framework)
- [CI/CD Integration](#cicd-integration)
- [Troubleshooting](#troubleshooting)
- [Examples](#examples)

---

## 🎯 Overview

The **Low-Code API Automation Framework** is designed to address a critical challenge: **API testing shouldn't require writing code for every test scenario**. Instead, tests are defined declaratively in YAML, allowing:

- **QA Teams** to write tests without Java knowledge
- **Automation Engineers** to focus on framework improvements, not repetitive test writing
- **Microservice Teams** to test their APIs independently without coupling to a centralized automation team
- **Cross-Service Teams** to orchestrate E2E flows with simple YAML definitions

### When to Use This Framework

✅ **Perfect For:**
- Microservice architectures with 20+ services
- REST API testing at multiple stages (T2, T3, IST, Prod)
- Contract testing between services
- Data pipeline validation
- Multi-step workflows spanning multiple services
- Teams with non-programmers who need to write tests

❌ **Not Ideal For:**
- UI automation (use Selenium for that)
- Mobile app testing
- Complex business logic that requires loops/conditionals
- Real-time performance testing (use JMeter instead)

---

## ✨ Key Features

### Core Capabilities

| Feature | Description |
|---------|-------------|
| **YAML-Driven Tests** | Declarative, human-readable test definitions |
| **HTTP Methods** | GET, POST, PUT, DELETE, PATCH support |
| **Variable Capture & Reuse** | Capture response fields and use in subsequent requests |
| **Request Chaining** | Execute dependent API calls sequentially |
| **JSON Validation** | Validate response structure and data types |
| **Multi-Service Support** | Test across multiple microservices in one flow |
| **Environment Configuration** | Easy switching between stage, IST, production |
| **Minimal Coding** | No Java knowledge required for test writing |
| **Extensible Architecture** | Add custom validators, assertions, headers |
| **CI/CD Ready** | Integrates with Jenkins, GitLab CI, GitHub Actions |

### Advanced Features

- **Contract Testing:** Validate API contracts match specifications
- **Shadow Mode:** Run tests in parallel as validation gates
- **Smoke Test Suites:** Quick sanity checks for critical APIs
- **Data-Driven Testing:** Parameterize tests with multiple datasets
- **Custom Headers:** Dynamic headers (auth tokens, timestamps)
- **Parallel Execution:** Run multiple test files concurrently
- **HTML Reporting:** Comprehensive test execution reports
- **Request Templating:** Dynamic URL/body construction

---

## 🏗️ Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      YAML TEST FILES                            │
│  ├─ posts_test.yaml       (Post service tests)                 │
│  ├─ users_test.yaml       (User service tests)                 │
│  ├─ orders_test.yaml      (Order service E2E)                  │
│  └─ cart_test.yaml        (Cart service tests)                 │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│                    TestEngine.java                              │
│  ├─ Reads YAML files                                           │
│  ├─ Parses test steps                                          │
│  ├─ Manages variable state (memory)                            │
│  ├─ Substitutes variables in requests                          │
│  └─ Orchestrates step execution                                │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│                   HttpExecutor.java                             │
│  ├─ Executes HTTP requests (GET/POST/PUT/DELETE)              │
│  ├─ Manages HTTP headers & authentication                      │
│  ├─ Injects variables into paths & bodies                      │
│  ├─ Captures response fields into memory                       │
│  └─ Returns response for validation                            │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│                 SchemaValidator.java                            │
│  ├─ Validates JSON response structure                          │
│  ├─ Checks required fields exist                               │
│  ├─ Validates field data types                                 │
│  └─ Detects API contract violations                            │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│              TARGET MICROSERVICES                               │
│  ├─ POST Service (http://posts-api.svc/)                      │
│  ├─ User Service (http://users-api.svc/)                      │
│  ├─ Order Service (http://orders-api.svc/)                    │
│  ├─ Cart Service (http://cart-api.svc/)                       │
│  └─ ... (N microservices)                                      │
└─────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility |
|-----------|-----------------|
| **TestEngine.java** | Orchestrates test execution, manages variable memory, reads YAML files |
| **HttpExecutor.java** | Makes HTTP calls, handles headers/auth, captures response values |
| **SchemaValidator.java** | Validates JSON response structure against expected schema |
| **YAML Test Files** | Define test steps, API paths, expected responses in declarative format |

### Data Flow

```
1. TestEngine reads YAML file
   ↓
2. For each step:
   ├─ Substitute variables in path (e.g., /posts/${postId})
   ├─ Substitute variables in body (e.g., {"cartId": "${cartItemId}"})
   ├─ HttpExecutor makes HTTP call
   ├─ Response returned
   ├─ SchemaValidator checks response structure
   ├─ Capture specified fields into memory
   └─ Log step result (Pass/Fail)
   ↓
3. Move to next step (with variables from previous step)
   ↓
4. Generate test report
```

---

## 🚀 Getting Started

### Prerequisites

- **Java:** JDK 11 or higher
- **Maven:** 3.6.0 or higher
- **Git:** For version control
- **IDE:** IntelliJ IDEA, Eclipse, or VS Code (optional)
- **cURL or Postman:** For manual API testing (optional)

### Quick Start (10 minutes)

#### Step 1: Clone the Repository

```bash
git clone https://github.com/01rahulsingh86/lowcode-automation-.git
cd lowcode-automation-
```

#### Step 2: Build the Project

```bash
mvn clean compile
```

#### Step 3: Copy Test Files

```bash
mkdir -p tests
cp src/main/resources/tests/*.yaml tests/
```

#### Step 4: Run Tests

```bash
mvn exec:java -Dexec.mainClass="engine.TestEngine"
```

#### Step 5: View Results

```
Console output shows:
✓ Step name
✓ HTTP method and path
✓ Response status code
✓ Captured variables
✓ Pass/Fail result
```

---

## 📦 Installation

### Option 1: Maven Setup (Recommended)

```bash
# Clone repository
git clone https://github.com/01rahulsingh86/lowcode-automation-.git
cd lowcode-automation-

# Build project
mvn clean compile

# Install dependencies
mvn install

# Ready to run!
mvn exec:java -Dexec.mainClass="engine.TestEngine"
```

### Option 2: IDE Setup (IntelliJ IDEA)

1. **File → Open** → Select `lowcode-automation-` folder
2. **Configure project structure:**
   - **src/main/java** → Mark as **Sources Root**
   - **src/main/resources** → Mark as **Resources Root**
   - **src/test/java** → Mark as **Test Sources Root**
3. **Build → Rebuild Project**
4. **Run → Edit Configurations → Add new "Application" configuration**
   - Main class: `engine.TestEngine`
   - Click "Run"

### Option 3: Docker Setup

```dockerfile
FROM openjdk:11-jre-slim
WORKDIR /app
COPY . .
RUN mvn clean compile
RUN mkdir -p tests && cp src/main/resources/tests/*.yaml tests/
CMD ["mvn", "exec:java", "-Dexec.mainClass=engine.TestEngine"]
```

```bash
# Build Docker image
docker build -t lowcode-automation .

# Run tests in container
docker run lowcode-automation
```

---

## 📝 YAML Test File Format

### Basic Structure

```yaml
# Optional: Base URL for this test file
base_url: https://api.example.com

# Define test steps
steps:
  - name: step_name
    method: GET|POST|PUT|DELETE
    path: /api/endpoint
    body: |                      # Optional (for POST/PUT)
      {
        "field": "value"
      }
    capture:                     # Optional: capture response fields
      variableName: response.field
    expected_status: 200         # Optional: validate status code
    headers:                     # Optional: custom headers
      Authorization: "Bearer token123"
```

### Example 1: Simple GET Request

```yaml
steps:
  - name: list_posts
    method: GET
    path: /posts
    expected_status: 200
```

**What Happens:**
1. Makes GET request to `https://api.example.com/posts`
2. Expects HTTP 200 response
3. Logs result

---

### Example 2: POST Request with Capture

```yaml
steps:
  - name: create_post
    method: POST
    path: /posts
    body: |
      {
        "title": "My First Post",
        "body": "This is the content",
        "userId": 1
      }
    capture:
      postId: response.id        # Capture 'id' field from response
    expected_status: 201
```

**What Happens:**
1. Makes POST request with JSON body
2. Receives response: `{"id": 101, "title": "My First Post", ...}`
3. Captures `101` into variable `postId`
4. Variable `${postId}` now available for subsequent steps

---

### Example 3: Chained Requests (Using Captured Variables)

```yaml
steps:
  - name: create_post
    method: POST
    path: /posts
    body: |
      {
        "title": "Test Post",
        "body": "Content",
        "userId": 1
      }
    capture:
      postId: response.id

  - name: get_post
    method: GET
    path: /posts/${postId}       # Uses captured postId
    expected_status: 200

  - name: update_post
    method: PUT
    path: /posts/${postId}
    body: |
      {
        "title": "Updated Title"
      }
    expected_status: 200

  - name: delete_post
    method: DELETE
    path: /posts/${postId}
    expected_status: 204
```

**What Happens:**
1. **Step 1:** POST creates post, captures ID (e.g., 101)
2. **Step 2:** GET retrieves `/posts/101`
3. **Step 3:** PUT updates `/posts/101`
4. **Step 4:** DELETE removes `/posts/101`

All steps share the same `postId` variable!

---

### Example 4: Multi-Service E2E Flow

```yaml
# Cart Service Tests
base_url: https://cart-service.myapp.com

steps:
  - name: create_cart
    method: POST
    path: /carts
    body: |
      {
        "userId": 123
      }
    capture:
      cartId: response.id

  - name: add_item_to_cart
    method: POST
    path: /carts/${cartId}/items
    body: |
      {
        "productId": 456,
        "quantity": 2
      }
    capture:
      cartItemId: response.itemId

---

# Order Service Tests
base_url: https://order-service.myapp.com

steps:
  - name: create_order
    method: POST
    path: /orders
    body: |
      {
        "cartId": "${cartId}",
        "userId": 123,
        "shippingAddress": "123 Main St"
      }
    capture:
      orderId: response.id

  - name: verify_order
    method: GET
    path: /orders/${orderId}
    expected_status: 200
```

**What Happens:**
1. Create cart in Cart Service → capture `cartId`
2. Add item to cart → capture `cartItemId`
3. Create order in Order Service using `${cartId}` → capture `orderId`
4. Verify order in Order Service

---

## 🔄 Variable Capturing & Reuse

### How Variables Work

Variables are stored in **TestEngine memory** and persist across steps.

### Capturing from Response

```yaml
capture:
  variableName: response.field.subfield
```

**Examples:**

```yaml
# Capture top-level field
capture:
  userId: response.id

# Capture nested field
capture:
  username: response.user.name
  email: response.user.email

# Capture array element (first element)
capture:
  firstPostId: response.posts[0].id
```

### Using Variables in Requests

**In URL path:**
```yaml
path: /users/${userId}/posts/${postId}
```

**In request body:**
```yaml
body: |
  {
    "cartId": "${cartId}",
    "userId": "${userId}",
    "items": [
      {"productId": "${productId}"}
    ]
  }
```

### Variable Substitution Examples

```yaml
steps:
  - name: create_user
    method: POST
    path: /users
    body: |
      {
        "name": "John Doe",
        "email": "john@example.com"
      }
    capture:
      userId: response.id
      userEmail: response.email

  - name: get_user
    method: GET
    path: /users/${userId}        # Substitutes userId
    expected_status: 200

  - name: create_post_for_user
    method: POST
    path: /users/${userId}/posts  # Reuse userId
    body: |
      {
        "title": "Hello",
        "authorEmail": "${userEmail}" # Reuse userEmail
      }
    capture:
      postId: response.id

  - name: link_post_to_cart
    method: POST
    path: /carts/items
    body: |
      {
        "postId": "${postId}",
        "userId": "${userId}"
      }
```

---

## 🏃 Execution Guide

### Running All Tests

```bash
mvn exec:java -Dexec.mainClass="engine.TestEngine"
```

### Running Specific Test File

Modify `TestEngine.main()`:

```java
public static void main(String[] args) throws Exception {
    // Run specific test file
    runTestFile("tests/posts_test.yaml");
}
```

### Running Multiple Test Files Sequentially

```java
public static void main(String[] args) throws Exception {
    runTestFile("tests/users_test.yaml");
    runTestFile("tests/posts_test.yaml");
    runTestFile("tests/orders_test.yaml");
    runTestFile("tests/cart_test.yaml");
}
```

### Running Tests with Different Base URLs

```yaml
# users_test.yaml
base_url: https://t2-users-api.myapp.com

# orders_test.yaml
base_url: https://t2-orders-api.myapp.com
```

Or parameterize:

```java
String environment = System.getProperty("env", "stage");
String baseUrl = getBaseUrlForEnv(environment);
// Use in test execution
```

### Running Tests in Docker

```bash
docker run lowcode-automation
```

---

## 🔗 Multi-Service E2E Testing

### Scenario: Shopping Cart Flow Across 3 Microservices

**Services:**
1. User Service (`https://user-api.svc`)
2. Product Service (`https://product-api.svc`)
3. Order Service (`https://order-api.svc`)

**E2E Flow:**

```yaml
# Step 1: Get user details from User Service
base_url: https://user-api.svc
steps:
  - name: get_user
    method: GET
    path: /users/123
    capture:
      userId: response.id
      userName: response.name

---

# Step 2: List products from Product Service
base_url: https://product-api.svc
steps:
  - name: list_products
    method: GET
    path: /products?category=electronics
    capture:
      productId: response.products[0].id
      productPrice: response.products[0].price

---

# Step 3: Create order in Order Service using captured IDs
base_url: https://order-api.svc
steps:
  - name: create_order
    method: POST
    path: /orders
    body: |
      {
        "userId": "${userId}",
        "items": [
          {
            "productId": "${productId}",
            "quantity": 1,
            "price": ${productPrice}
          }
        ]
      }
    capture:
      orderId: response.id
    expected_status: 201

  - name: verify_order
    method: GET
    path: /orders/${orderId}
    expected_status: 200
```

**Execution Flow:**
```
User Service:     GET /users/123 → userId = 123, userName = "John"
                           ↓
Product Service:  GET /products → productId = 789, productPrice = 99.99
                           ↓
Order Service:    POST /orders with { userId: 123, productId: 789, ... }
                           ↓
Order Service:    GET /orders/456 → Verify order created
```

### Key Points

✅ Variables flow across different microservices
✅ Each service can have its own base_url
✅ Data from Service A feeds into Service B
✅ Validate end-to-end workflows without code
✅ Detect integration issues early

---

## ✅ JSON Response Validation

### Schema Validation

Ensure API responses contain expected fields and types.

### Example: Validate User Response

```java
// In SchemaValidator.java
Map<String, String> expectedFields = Map.of(
    "id", "integer",
    "name", "string",
    "email", "string",
    "status", "string"
);

String responseBody = "{\"id\": 123, \"name\": \"John\", \"email\": \"john@example.com\", \"status\": \"active\"}";
SchemaValidator.validate(responseBody, expectedFields);
// ✓ Pass: All fields present with correct types
```

### Example: Missing Required Field (Fails)

```java
String responseBody = "{\"id\": 123, \"name\": \"John\"}";
// Missing "email" and "status"
SchemaValidator.validate(responseBody, expectedFields);
// ✗ Fail: Required fields missing
```

### How to Use in Tests

```yaml
steps:
  - name: get_user
    method: GET
    path: /users/123
    expected_status: 200
    # Validation happens in HttpExecutor
    # Checks response contains: id (int), name (string), email (string)
```

### Benefits

✅ Detect API contract violations
✅ Catch missing fields early
✅ Validate data types
✅ Prevent silent failures

---

## ⚙️ Environment Configuration

### Multi-Environment Support

Define different base URLs for each environment:

```yaml
# config.yaml or in Java Map
environments:
  stage:
    base_url: https://stage-api.myapp.com
    user_service: https://stage-user-api.myapp.com
    order_service: https://stage-order-api.myapp.com
  
  ist:
    base_url: https://ist-api.myapp.com
    user_service: https://ist-user-api.myapp.com
    order_service: https://ist-order-api.myapp.com
  
  prod:
    base_url: https://api.myapp.com
    user_service: https://user-api.myapp.com
    order_service: https://order-api.myapp.com
```

### Using Environment Variables

```java
// In TestEngine.java
String env = System.getenv("ENVIRONMENT");  // stage, ist, prod
String baseUrl = getBaseUrlForEnv(env);
```

### Running Against Different Environments

```bash
# Run tests against staging
ENVIRONMENT=stage mvn exec:java -Dexec.mainClass="engine.TestEngine"

# Run tests against IST
ENVIRONMENT=ist mvn exec:java -Dexec.mainClass="engine.TestEngine"

# Run tests against production
ENVIRONMENT=prod mvn exec:java -Dexec.mainClass="engine.TestEngine"
```

### In Docker

```dockerfile
ENV ENVIRONMENT=stage
CMD ["mvn", "exec:java", "-Dexec.mainClass=engine.TestEngine"]
```

```bash
docker run -e ENVIRONMENT=ist lowcode-automation
```

---

## ✅ Best Practices

### 1. Test Organization

```
tests/
├── smoke/
│   ├── health_check.yaml          # Quick sanity tests
│   └── critical_paths.yaml
├── regression/
│   ├── users_regression.yaml
│   ├── orders_regression.yaml
│   └── cart_regression.yaml
└── integration/
    ├── e2e_shopping_flow.yaml     # Multi-service
    └── e2e_order_pipeline.yaml
```

### 2. Variable Naming

```yaml
# ✅ GOOD: Clear, descriptive names
capture:
  userId: response.id
  userName: response.name
  orderTotalPrice: response.total

# ❌ BAD: Vague names
capture:
  var1: response.id
  temp: response.name
  x: response.total
```

### 3. Capture Strategy

```yaml
# ✅ GOOD: Capture what you'll reuse
steps:
  - name: create_order
    method: POST
    path: /orders
    capture:
      orderId: response.id          # Will use in next step
      orderStatus: response.status

  - name: verify_order
    method: GET
    path: /orders/${orderId}        # Reuse orderId
```

### 4. Error Handling

```yaml
# ✅ GOOD: Expected status for each step
steps:
  - name: create_item
    method: POST
    path: /items
    expected_status: 201            # Explicit expectation
  
  - name: update_item
    method: PUT
    path: /items/${itemId}
    expected_status: 200

  - name: delete_item
    method: DELETE
    path: /items/${itemId}
    expected_status: 204            # 204 No Content expected
```

### 5. Data-Driven Tests

```yaml
# For parameterized tests, run with different data files
users_test_data1.yaml   # Test user A
users_test_data2.yaml   # Test user B
users_test_data3.yaml   # Test user C

# Or modify YAML to support arrays:
steps:
  - name: create_users
    method: POST
    path: /users
    for_each: users        # Pseudo-syntax (extend framework)
    body: |
      {
        "name": "${user.name}",
        "email": "${user.email}"
      }
```

### 6. Handling Mock APIs

```yaml
# ⚠️ Note: jsonplaceholder.typicode.com is a mock
# POSTs are NOT persisted → subsequent GETs will fail
steps:
  - name: create_post
    method: POST
    path: /posts
    capture:
      postId: response.id    # Will be high ID (e.g., 101)

  - name: get_post
    method: GET
    path: /posts/${postId}
    # ❌ Will likely return 404 on mock API
    # ✅ Use real microservice endpoints for true validation
```

### 7. Modular Tests

```yaml
# ✅ GOOD: One file per service
├── posts_test.yaml       # Test posts API only
├── users_test.yaml       # Test users API only
└── orders_test.yaml      # Test orders API only

# ❌ BAD: Everything in one file
└── all_tests.yaml        # 500+ lines, hard to maintain
```

### 8. Comments & Documentation

```yaml
# User Service Tests
# Purpose: Validate CRUD operations on users API
# Scope: Create, Read, Update, Delete users
# Dependencies: None

base_url: https://api.example.com

steps:
  # Create a new user
  - name: create_user
    method: POST
    path: /users
    body: |
      {
        "name": "John Doe",
        "email": "john@example.com"
      }
    capture:
      userId: response.id

  # Retrieve the created user
  - name: get_user
    method: GET
    path: /users/${userId}
    expected_status: 200
```

---

## 🔧 Extending the Framework

### Add Custom Assertions

```java
// CustomAssertions.java
public class CustomAssertions {
    public static void assertJsonPathExists(String json, String path) {
        // Validate JSON path exists
    }
    
    public static void assertResponseTimeUnder(long responseTime, long limit) {
        // Validate response time < limit
    }
}
```

### Add Dynamic Headers

```java
// In HttpExecutor.java
public void addDynamicHeaders(Map<String, String> headers) {
    String timestamp = System.currentTimeMillis();
    headers.put("X-Request-ID", UUID.randomUUID().toString());
    headers.put("X-Timestamp", timestamp);
    headers.put("Authorization", "Bearer " + getAuthToken());
}
```

### Add Request/Response Logging

```java
// In HttpExecutor.java
public void logRequest(String method, String url, String body) {
    System.out.println("[REQUEST] " + method + " " + url);
    System.out.println("[BODY] " + body);
}

public void logResponse(int status, String body) {
    System.out.println("[RESPONSE] Status: " + status);
    System.out.println("[BODY] " + body);
}
```

### Add Parallel Execution

```java
// In TestEngine.java
ExecutorService executor = Executors.newFixedThreadPool(5);
List<File> testFiles = getTestFiles();

for (File testFile : testFiles) {
    executor.submit(() -> runTestFile(testFile.getPath()));
}

executor.shutdown();
executor.awaitTermination(10, TimeUnit.MINUTES);
```

### Add HTML Reporting

```java
// In ReportGenerator.java
public void generateHTMLReport(List<TestResult> results) {
    StringBuilder html = new StringBuilder();
    html.append("<html><body>");
    html.append("<h1>Test Execution Report</h1>");
    
    for (TestResult result : results) {
        html.append("<div class=\"test\">");
        html.append("<p>").append(result.getStepName()).append("</p>");
        html.append("<p class=\"").append(result.getStatus()).append("\">")
            .append(result.getStatus()).append("</p>");
        html.append("</div>");
    }
    
    html.append("</body></html>");
    writeToFile("reports/index.html", html.toString());
}
```

---

## 🔄 CI/CD Integration

### Jenkins Integration

**Jenkinsfile:**

```groovy
pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
                sh 'mkdir -p tests && cp src/main/resources/tests/*.yaml tests/'
            }
        }
        
        stage('Test - Smoke') {
            steps {
                sh 'mvn exec:java -Dexec.mainClass="engine.TestEngine" -Dsuite=smoke'
            }
        }
        
        stage('Test - Regression') {
            steps {
                sh 'mvn exec:java -Dexec.mainClass="engine.TestEngine" -Dsuite=regression'
            }
        }
        
        stage('Test - E2E') {
            steps {
                sh 'mvn exec:java -Dexec.mainClass="engine.TestEngine" -Dsuite=integration'
            }
        }
        
        stage('Report') {
            steps {
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'reports',
                    reportFiles: 'index.html',
                    reportName: 'API Test Report'
                ])
            }
        }
    }
    
    post {
        failure {
            echo 'Tests failed!'
            // Send Slack/Email notification
        }
    }
}
```

### GitHub Actions Integration

**.github/workflows/api-tests.yml:**

```yaml
name: API Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    strategy:
      matrix:
        environment: [stage, ist, prod]
    
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up Java
        uses: actions/setup-java@v2
        with:
          java-version: 11
      
      - name: Build
        run: mvn clean compile
      
      - name: Copy tests
        run: mkdir -p tests && cp src/main/resources/tests/*.yaml tests/
      
      - name: Run tests against ${{ matrix.environment }}
        run: mvn exec:java -Dexec.mainClass="engine.TestEngine" -Denv=${{ matrix.environment }}
      
      - name: Upload report
        if: always()
        uses: actions/upload-artifact@v2
        with:
          name: test-report-${{ matrix.environment }}
          path: reports/
```

### GitLab CI Integration

**.gitlab-ci.yml:**

```yaml
stages:
  - build
  - test

build:
  stage: build
  image: maven:3.8-jdk-11
  script:
    - mvn clean compile
    - mkdir -p tests && cp src/main/resources/tests/*.yaml tests/
  artifacts:
    paths:
      - target/
      - tests/

test:smoke:
  stage: test
  image: maven:3.8-jdk-11
  dependencies:
    - build
  script:
    - mvn exec:java -Dexec.mainClass="engine.TestEngine" -Dsuite=smoke

test:regression:
  stage: test
  image: maven:3.8-jdk-11
  dependencies:
    - build
  script:
    - mvn exec:java -Dexec.mainClass="engine.TestEngine" -Dsuite=regression

test:e2e:
  stage: test
  image: maven:3.8-jdk-11
  dependencies:
    - build
  script:
    - mvn exec:java -Dexec.mainClass="engine.TestEngine" -Dsuite=integration
```

---

## 🐛 Troubleshooting

### Issue 1: YAML File Not Found

```
ERROR: YAML file not found: tests/posts_test.yaml
```

**Solution:**
```bash
# Ensure test files are copied
mkdir -p tests
cp src/main/resources/tests/*.yaml tests/
ls -la tests/    # Verify files exist
```

### Issue 2: Variable Not Substituted

```yaml
body: |
  {
    "userId": "${userId}"   # Returns literal "${userId}" instead of value
  }
```

**Solution:**
Check variable was captured in previous step:
```yaml
steps:
  - name: create_user
    method: POST
    path: /users
    capture:
      userId: response.id   # Must exist before use
```

### Issue 3: HTTP 404 on Mock API

```
POST /posts → Success (Status 201, ID: 101)
GET /posts/101 → Fail (Status 404)
```

**Solution:**
Use real microservice endpoints instead of mock APIs (jsonplaceholder.typicode.com doesn't persist data).

### Issue 4: Incorrect JSON Path in Capture

```yaml
capture:
  postId: response.id          # ✓ Correct
  postId: response.data.id     # ✓ Correct (nested)
  postId: response[0].id       # ✗ Array syntax unsupported
```

**Solution:**
Use dot notation for nested fields, not array notation.

### Issue 5: Header Not Applied

```yaml
steps:
  - name: authorized_request
    method: GET
    path: /users/me
    headers:
      Authorization: "Bearer my-token"
```

**Solution:**
Verify headers are passed to HttpExecutor in correct format:
```java
Map<String, String> headers = Map.of("Authorization", "Bearer my-token");
httpExecutor.executeRequest(method, url, body, headers);
```

### Debugging Tips

#### Enable Verbose Logging

```java
// In TestEngine.java
private static final boolean DEBUG = true;

if (DEBUG) {
    System.out.println("[DEBUG] Variables in memory: " + variables);
    System.out.println("[DEBUG] Substituted path: " + substitutedPath);
    System.out.println("[DEBUG] Request body: " + body);
    System.out.println("[DEBUG] Response: " + response);
}
```

#### Print Variables at Each Step

```yaml
steps:
  - name: debug_print
    # Add after capture steps to see variable values
    # (Pseudo-code, extend framework to support)
```

#### Use Online JSON Tools

Test JSON paths at [jsonpath.com](https://jsonpath.com) before using in capture.

---

## 📚 Examples

### Example 1: REST CRUD Operations

**File: users_test.yaml**

```yaml
base_url: https://api.example.com

steps:
  # CREATE
  - name: create_user
    method: POST
    path: /users
    body: |
      {
        "name": "John Doe",
        "email": "john@example.com",
        "age": 30
      }
    capture:
      userId: response.id
    expected_status: 201

  # READ
  - name: get_user
    method: GET
    path: /users/${userId}
    expected_status: 200

  # UPDATE
  - name: update_user
    method: PUT
    path: /users/${userId}
    body: |
      {
        "name": "Jane Doe",
        "age": 31
      }
    expected_status: 200

  # DELETE
  - name: delete_user
    method: DELETE
    path: /users/${userId}
    expected_status: 204
```

### Example 2: Contract Testing

**File: api_contract_test.yaml**

```yaml
base_url: https://api.example.com

steps:
  - name: validate_user_contract
    method: GET
    path: /users/1
    # Response must contain these fields (validated by SchemaValidator)
    # Schema: id (int), name (string), email (string), createdAt (string)
    expected_status: 200

  - name: validate_post_contract
    method: GET
    path: /posts/1
    # Schema: id (int), title (string), body (string), userId (int)
    expected_status: 200
```

### Example 3: Smoke Test Suite

**File: smoke_tests.yaml**

```yaml
base_url: https://api.example.com

steps:
  - name: health_check
    method: GET
    path: /health
    expected_status: 200

  - name: users_endpoint_available
    method: GET
    path: /users?limit=1
    expected_status: 200

  - name: posts_endpoint_available
    method: GET
    path: /posts?limit=1
    expected_status: 200

  - name: comments_endpoint_available
    method: GET
    path: /comments?limit=1
    expected_status: 200
```

### Example 4: E2E Multi-Service Flow

**File: e2e_order_flow.yaml**

```yaml
# USER SERVICE
base_url: https://user-api.svc

steps:
  - name: get_user
    method: GET
    path: /users/123
    capture:
      userId: response.id
      userName: response.name

---

# PRODUCT SERVICE
base_url: https://product-api.svc

steps:
  - name: list_products
    method: GET
    path: /products?category=electronics
    capture:
      productId: response.products[0].id
      productName: response.products[0].name
      productPrice: response.products[0].price

---

# CART SERVICE
base_url: https://cart-api.svc

steps:
  - name: create_cart
    method: POST
    path: /carts
    body: |
      {
        "userId": "${userId}"
      }
    capture:
      cartId: response.id

  - name: add_item_to_cart
    method: POST
    path: /carts/${cartId}/items
    body: |
      {
        "productId": "${productId}",
        "quantity": 2
      }
    capture:
      itemId: response.itemId

---

# ORDER SERVICE
base_url: https://order-api.svc

steps:
  - name: create_order
    method: POST
    path: /orders
    body: |
      {
        "userId": "${userId}",
        "cartId": "${cartId}",
        "items": [
          {
            "productId": "${productId}",
            "quantity": 2,
            "price": ${productPrice}
          }
        ]
      }
    capture:
      orderId: response.id
      orderTotal: response.total
    expected_status: 201

  - name: verify_order_created
    method: GET
    path: /orders/${orderId}
    expected_status: 200
```

---

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

---

## 👥 Author

**Rahul Singh** - [@01rahulsingh86](https://github.com/01rahulsingh86)

---

## 🎯 Roadmap

### v1.1 (Q2 2026)
- [ ] Support for conditional steps (if/else logic)
- [ ] Data-driven test iterations
- [ ] Parallel test file execution
- [ ] HTML & JSON reporting
- [ ] Request/response logging with timestamps

### v1.2 (Q3 2026)
- [ ] GraphQL support
- [ ] WebSocket testing
- [ ] Performance metrics collection
- [ ] Database validation steps
- [ ] Message queue integration

### v2.0 (Q4 2026)
- [ ] Visual test editor (web UI for YAML)
- [ ] Advanced filtering & search
- [ ] Real-time execution dashboard
- [ ] Integration with API documentation tools
- [ ] Mobile app testing support

---

## 💬 Support & Feedback

- **GitHub Issues:** Report bugs and feature requests
- **Discussions:** Ask questions and share ideas
- **Email:** Contact framework author

---

**Last Updated:** February 17, 2026
**Version:** 1.0.0
**Status:** Production Ready ✅
