package engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.restassured.response.Response;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SchemaValidator {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void validate(Map<String, Object> step, Response response) {
        ensureJsonResponse(response);

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

    public static void validateSchemaFile(String schemaFile, Response response) {
        ensureJsonResponse(response);

        try {
            Path schemaPath = JsonUtils.resolvePath(schemaFile);
            JsonNode schemaNode = OBJECT_MAPPER.readTree(Files.readString(schemaPath));
            JsonNode responseNode = OBJECT_MAPPER.readTree(response.getBody().asString());

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            JsonSchema schema = factory.getSchema(schemaNode);
            Set<ValidationMessage> validationErrors = schema.validate(responseNode);

            if (!validationErrors.isEmpty()) {
                String message = validationErrors.stream()
                        .limit(5)
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining("; "));
                throw new RuntimeException("JSON Schema validation failed (" + schemaFile + "): " + message);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate schema file '" + schemaFile + "': " + e.getMessage(), e);
        }
    }

    private static void ensureJsonResponse(Response response) {
        String contentType = response.contentType();
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            throw new RuntimeException("Response is not JSON. Content-Type: " + contentType);
        }
    }
}
