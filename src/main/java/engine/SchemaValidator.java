package engine;

import io.restassured.response.Response;

import java.util.Map;

public class SchemaValidator {

    public static void validate(Map<String, Object> step, Response response) {

        // 🔒 Safety: ensure JSON
        if (!response.contentType().contains("application/json")) {
            throw new RuntimeException(
                "Response is not JSON. Content-Type: " + response.contentType()
            );
        }

        Map<String, Object> assertions =
                (Map<String, Object>) step.get("assert");

        assertions.forEach((field, rules) -> {

            Object value = response.jsonPath().get(field);

            if (value == null) {
                throw new RuntimeException("Missing field: " + field);
            }

            Map<String, Object> ruleMap = (Map<String, Object>) rules;
            String expectedType = (String) ruleMap.get("type");

            switch (expectedType) {

                case "number":
                    double numericValue;
                    try {
                        numericValue = Double.parseDouble(value.toString());
                    } catch (Exception e) {
                        throw new RuntimeException(
                            "Field '" + field + "' is not numeric. Value: " + value
                        );
                    }

                    if (ruleMap.containsKey("min")) {
                        double min = ((Number) ruleMap.get("min")).doubleValue();
                        if (numericValue < min) {
                            throw new RuntimeException(
                                "Validation failed for " + field +
                                ". Expected >= " + min + ", actual=" + numericValue
                            );
                        }
                    }
                    break;

                case "string":
                    if (!(value instanceof String)) {
                        throw new RuntimeException(
                            "Field '" + field + "' is not a string"
                        );
                    }
                    break;

                default:
                    throw new RuntimeException("Unsupported assertion type: " + expectedType);
            }
        });
    }
}

