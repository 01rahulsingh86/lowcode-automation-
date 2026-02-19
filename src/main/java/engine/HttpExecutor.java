package engine;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

public class HttpExecutor {

    // Context shared across all steps (for variable chaining)
    private static Map<String, Object> context;

    public static void setContext(Map<String, Object> ctx) {
        context = ctx;
    }

    // Replace ${var} placeholders with actual captured values
    private static String resolveVariables(String value) {
        if (value == null) return null;
        if (context == null) return value;

        for (String key : context.keySet()) {
            Object val = context.get(key);
            if (val == null) {
                throw new RuntimeException("Variable '" + key + "' is null but used in step.");
            }
            value = value.replace("${" + key + "}", val.toString());
        }
        return value;
    }

    /**
     * Execute a single step.
     *
     * @param step    YAML-defined step (method, path, body, capture, baseUrl)
     * @param defaultBaseUrl Default base URL if step does not define one
     * @return RestAssured Response
     */
    public static Response execute(Map<String, Object> step, String defaultBaseUrl) throws InterruptedException {
        String method = (String) step.get("method");
        String path = resolveVariables((String) step.get("path"));
        String baseUrlStep = step.containsKey("baseUrl") ? (String) step.get("baseUrl") : defaultBaseUrl;

        // Resolve body variables
        Object bodyObj = step.get("body");
        if (bodyObj instanceof Map) {
            Map<String, Object> bodyMap = (Map<String, Object>) bodyObj;
            for (String key : bodyMap.keySet()) {
                Object val = bodyMap.get(key);
                if (val instanceof String) {
                    bodyMap.put(key, resolveVariables((String) val));
                }
            }
            bodyObj = bodyMap;
        }

        // Build request spec
        RequestSpecification spec = RestAssured.given()
                .baseUri(baseUrlStep)
                .contentType("application/json")
                .accept("application/json");

        if (bodyObj != null) {
            spec.body(bodyObj);
        }

        // Optional retry for eventual consistency (e.g., cross-service GET)
        int attempts = 0;
        Response response;
        int maxRetries = step.containsKey("retry") ? (Integer) step.get("retry") : 1;
        int waitMs = step.containsKey("waitMs") ? (Integer) step.get("waitMs") : 500;

        do {
            response = spec.request(method, path);
            if (response.statusCode() < 400) break; // success
            Thread.sleep(waitMs);
            attempts++;
        } while (attempts < maxRetries);

        // Debug logs
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Response Body: " + response.asString());
        System.out.println("Response Content-Type: " + response.contentType());

        // Capture variables
        Map<String, Object> capture = (Map<String, Object>) step.get("capture");
        if (capture != null) {
            for (String key : capture.keySet()) {
                String jsonPath = (String) capture.get(key);
                Object val = response.jsonPath().get(jsonPath.replace("response.", ""));
                if (val == null) {
                    System.out.println("Warning: Captured value is null for key: " + key);
                } else {
                    context.put(key, val.toString());
                    System.out.println("Captured: " + key + " = " + val);
                }
            }
        }

        return response;
    }
}

