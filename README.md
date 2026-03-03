# Low-Code API Test Automation Framework

A lightweight YAML-driven API test runner built with Java + Maven.

## Features
- Run API test flows from YAML files
- Support chained variables like `${orderId}`
- Support common top-level headers plus step-level header overrides
- Data-driven execution via `data_file` (`.csv`, `.json`, `.yaml`, `.yml`)
- Load request payloads from external body files (`body_file` / `bodyFile`) including JSON and YAML
- Capture response values using JSONPath (`capture`)
- Built-in dynamic placeholders like `${uuid}`, `${timestamp}`, `${randomInt}`
- Optional schema checks (`expectedSchema`)
- Full JSON Schema validation via `schema_file` / `schemaFile`
- Generate HTML report at `target/test-report.html`
- Request/response details shown in collapsible sections in the report

## Tech Stack
- Java 17
- Maven
- Rest Assured
- SnakeYAML
- Jackson

## Project Structure
```text
src/main/java/engine/
  TestEngine.java
  CurlToYamlConverter.java
  HttpExecutor.java
  JsonUtils.java
  SchemaValidator.java
  ReportGenerator.java

src/main/resources/
  tests/
    cart_flow.yaml
    cart_flow_data.yaml
    session_test.yaml
    order_flow.yaml
  data/
    cart_flow_data.csv
  payloads/
    add_to_cart.json
    submit_order.json
  schemas/
    post_response.schema.json
```

## Prerequisites
- Java 17 installed
- Maven installed

Check versions:
```bash
java -version
mvn -version
```

## Run Tests

Compile:
```bash
mvn -q -DskipTests compile
```

Run a single YAML file:
```bash
mvn -q -Dexec.mainClass=engine.TestEngine -Dexec.args="src/main/resources/tests/cart_flow.yaml" exec:java
```

Run data-driven cart flow (10 rows from CSV):
```bash
mvn -q -Dexec.mainClass=engine.TestEngine -Dexec.args="src/main/resources/tests/cart_flow_data.yaml" exec:java
```

Run default test files configured in `TestEngine.java`:
```bash
mvn -q -Dexec.mainClass=engine.TestEngine exec:java
```

Run multiple YAML files:
```bash
mvn -q -Dexec.mainClass=engine.TestEngine -Dexec.args="src/main/resources/tests/session_test.yaml src/main/resources/tests/order_flow.yaml" exec:java
```

## Convert `.curl` Files To YAML Tests

You can convert raw curl command files into runnable YAML test files.

1. Put curl commands into files ending with `.curl` (one command per file), for example:
`src/main/resources/curl/create_cart.curl`

2. Run converter:
```bash
mvn -q -Dexec.mainClass=engine.CurlToYamlConverter -Dexec.args="src/main/resources/curl src/main/resources/tests/generated" exec:java
```

3. Run generated YAML tests:
```bash
mvn -q -Dexec.mainClass=engine.TestEngine -Dexec.args="src/main/resources/tests/generated/create_cart.yaml" exec:java
```

Notes:
- Supported curl parts: URL, `-X/--request`, `-H/--header`, `-d/--data` (`--data-raw`, `--data-binary`, `--data-urlencode`)
- If method is missing and body exists, method defaults to `POST`; otherwise `GET`

## YAML Format

Top-level structure:
```yaml
base_url: https://jsonplaceholder.typicode.com
data_file: data/cart_flow_data.csv
steps:
  - name: add_to_cart
    method: POST
    path: /posts
    headers:
      Content-Type: "application/json"
    body_file: "payloads/add_to_cart.json"
    capture:
      cartId: response.id
```

Supported step fields:
- `name`: step name in logs/report
- `method`: `GET`, `POST`, `PUT`, `DELETE`
- `url`: full URL (alternative to `base_url + path`)
- `path`: endpoint path appended to `base_url`
- `headers`: key/value headers
- `body`: inline request body
- `body_file` or `bodyFile`: path to JSON payload file
- `capture`: map of variables to JSONPath expressions (prefix `response.` is optional)
- `expectedSchema`: optional schema/assertion config consumed by `SchemaValidator`
- `schema_file` or `schemaFile`: full JSON Schema file path for entire response validation

Supported top-level fields:
- `base_url`: base endpoint for `path`
- `headers`: common headers applied to every step (step headers override same key)
- `data_file`: optional data source for row-by-row execution (`.csv`, `.json`, `.yaml`, `.yml`)
- `steps`: list of request steps

Common header example:
```yaml
base_url: https://api.example.com
headers:
  Authorization: "Bearer ${token}"
  X-Client-Id: "${clientId}"

steps:
  - name: create_order
    method: POST
    path: /orders
    headers:
      X-Trace-Id: "${uuid}" # added only for this step
```

## Variable Chaining

Captured variables are reused in later steps using `${varName}`:
```yaml
capture:
  orderId: response.id

path: /posts/${orderId}
headers:
  X-Order-ID: "${orderId}"
```

Data-file variables are available the same way:
```yaml
data_file: data/cart_flow_data.csv

headers:
  X-Customer-Tag: "${customerTag}"
```

Built-in generated placeholders (no capture/data-file needed):
- `${uuid}` -> random UUID
- `${timestamp}` -> epoch milliseconds
- `${timestampSeconds}` -> epoch seconds
- `${randomInt}` -> random number from `0` to `999999`
- `${randomInt(min,max)}` -> random number in inclusive range

Example:
```yaml
headers:
  X-Request-Id: "${uuid}"
  X-Nonce: "${randomInt(1000,9999)}"
  X-Request-Time: "${timestamp}"
```

Notes:
- Generated placeholders are stable within one step (same value reused in URL/headers/body for that step)
- New step generates new values
- Captured/data-file values still take priority when names match

## Capture Nested JSON (Arrays/Objects)

`capture` supports nested JSONPath with array indexes.

Examples:
```yaml
capture:
  firstId: response.items[0].id
  secondUserName: response.users[1].name
  lineSku: response.data.orders[0].lines[1].sku
```

Notes:
- `response.` prefix is optional (`response.items[0].id` and `items[0].id` both work)
- Use `[index]` for arrays
- If a path is invalid or missing, the captured value becomes `null`
- Captured values are available in later steps as `${firstId}`, `${secondUserName}`, etc.

## Full JSON Schema Validation

Add `schema_file` at step level to validate the full response JSON:
```yaml
- name: create_post
  method: POST
  path: /posts
  body_file: payloads/add_to_cart.json
  schema_file: schemas/post_response.schema.json
```

Sample schema file:
`src/main/resources/schemas/post_response.schema.json`

Notes:
- `schema_file` supports variable replacement (e.g. `${schemaName}`)
- The response must be JSON
- If schema validation fails, the step is marked `FAIL` in report

## Payload Files

`body_file` paths are resolved in this order:
1. As provided (direct path)
2. Under `src/main/resources/`

Example:
```yaml
body_file: "payloads/add_to_cart.json"
```

Body file types:
- `.json` -> sent as-is (after variable replacement)
- `.yaml` / `.yml` -> converted to JSON, then sent
- other text files -> sent as plain text

## Data-Driven Files

`data_file` paths follow the same resolution rules as `body_file`.

Supported formats:
- CSV with header row
- JSON object or array of objects
- YAML map or list of maps

CSV example (`src/main/resources/data/cart_flow_data.csv`):
```csv
sessionId,customerTag
session-001,A
session-002,B
```

## HTML Report

After execution, open:
```text
target/test-report.html
```

Report includes:
- pass/fail summary
- one row per API step
- method, URL, status, duration
- collapsible request and response blocks (pretty JSON where applicable)
- captured variable view per step
- headers and captured maps shown with `:` format (JSON-style), not `=`

## Notes
- The engine currently treats HTTP status `< 400` as pass unless schema validation fails.
- Ensure your test endpoints return JSON if you use `capture` or schema checks.

## Troubleshooting

### 1) `404` or wrong endpoint behavior
Cause:
- Invalid `base_url` or `path`
- Endpoint does not exist in target API

Fix:
- Verify final URL in report/console logs
- Test URL quickly with curl:
```bash
curl -i https://jsonplaceholder.typicode.com/posts/1
```
- Update YAML `base_url` / `path` or use explicit `url`

### 2) `Failed to parse the JSON document` / response starts with `<html`
Cause:
- API returned HTML (error page) instead of JSON

Fix:
- Check status code and endpoint
- Ensure API returns JSON
- Avoid `capture`/schema checks on non-JSON responses

### 3) `JSON file not found: ...`
Cause:
- `body_file` path is incorrect

Fix:
- Confirm file exists relative to project root or under `src/main/resources/`
- Example valid value:
```yaml
body_file: "payloads/add_to_cart.json"
```

### 4) Variables not replacing (`${var}` remains unchanged)
Cause:
- Variable was never captured or capture path is wrong

Fix:
- Ensure previous step has `capture`
- Validate capture path against actual response JSON
- If using `data_file`, verify column/key names match placeholders exactly
- For nested arrays, confirm index exists (e.g. `items[1]` requires at least 2 items)
- Example:
```yaml
capture:
  orderId: response.id
```

### 5) `data_file` not loading / unsupported extension
Cause:
- Wrong path or unsupported file type

Fix:
- Use `.csv`, `.json`, `.yaml`, or `.yml`
- Check path exists under project root or `src/main/resources/`
- Example:
```yaml
data_file: data/cart_flow_data.csv
```

### 6) JSON Schema validation fails
Cause:
- Response does not match the schema
- Wrong schema path
- Response is not JSON

Fix:
- Check `schema_file` path and file content
- Verify required fields and data types in schema
- Confirm endpoint returns JSON
- Start with a minimal schema, then tighten gradually

### 7) Maven command fails on first run (dependency/plugin issues)
Cause:
- Local Maven cache/dependencies not resolved yet

Fix:
```bash
mvn -U -DskipTests compile
```
Then rerun:
```bash
mvn -q -Dexec.mainClass=engine.TestEngine -Dexec.args="src/main/resources/tests/cart_flow.yaml" exec:java
```

### 8) Report not updating
Cause:
- Test didn’t execute or wrote to a different location

Fix:
- Re-run command and confirm:
`Report generated: target/test-report.html`
- Open:
`target/test-report.html`

### 9) `Unknown host ...` while calling external API
Cause:
- DNS/network is unavailable from current environment

Fix:
- Verify internet/DNS access from your machine
- Try:
```bash
curl -i https://jsonplaceholder.typicode.com/posts/1
```
- Re-run Maven command once connectivity is available
