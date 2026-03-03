package engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

public class TestEngine {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    // Runtime variables include both data-file values and captured values.
    private static final Map<String, Object> runtimeVariables = new HashMap<>();

    // Default list of test YAML files to run
    private static final List<String> defaultTestFiles = Arrays.asList(
            "src/main/resources/tests/session_test.yaml",
            "src/main/resources/tests/order_flow.yaml"
    );

    public static void main(String[] args) throws Exception {
        List<String> testFiles = (args != null && args.length > 0)
                ? Arrays.asList(args)
                : defaultTestFiles;

        for (String yamlFile : testFiles) {
            runTestFile(yamlFile);
        }

        // Generate final HTML report
        ReportGenerator.generateReport("target/test-report.html");
        System.out.println("=== Test Execution Completed ===");
        System.out.println("Report generated: target/test-report.html");
    }

    @SuppressWarnings("unchecked")
    private static void runTestFile(String yamlFile) throws Exception {
        System.out.println("--- Running Test File: " + yamlFile + " ---");

        InputStream input = new FileInputStream(yamlFile);
        Yaml yaml = new Yaml();
        Object loaded = yaml.load(input);

        String baseUrl = "";
        List<Map<String, Object>> steps;
        Map<String, Object> defaultHeaders = Collections.emptyMap();
        List<Map<String, Object>> dataRows = Collections.singletonList(Collections.emptyMap());
        if (loaded instanceof List) {
            steps = (List<Map<String, Object>>) loaded;
        } else if (loaded instanceof Map) {
            Map<String, Object> root = (Map<String, Object>) loaded;
            Object baseUrlValue = root.get("base_url");
            if (baseUrlValue != null) {
                baseUrl = baseUrlValue.toString();
            }
            Object defaultHeadersValue = root.get("headers");
            if (defaultHeadersValue instanceof Map) {
                defaultHeaders = (Map<String, Object>) defaultHeadersValue;
            }
            steps = (List<Map<String, Object>>) root.get("steps");
            dataRows = loadDataRows((String) root.get("data_file"));
        } else {
            throw new IllegalArgumentException("Unsupported YAML format for " + yamlFile);
        }

        if (steps == null) {
            throw new IllegalArgumentException("No steps found in " + yamlFile);
        }

        for (int rowIndex = 0; rowIndex < dataRows.size(); rowIndex++) {
            Map<String, Object> dataRow = dataRows.get(rowIndex);
            runtimeVariables.clear();
            runtimeVariables.putAll(dataRow);

            String runLabel = dataRows.size() > 1
                    ? yamlFile + " [row " + (rowIndex + 1) + "]"
                    : yamlFile;
            ReportGenerator.startTestFile(runLabel);
            System.out.println("Dataset row " + (rowIndex + 1) + "/" + dataRows.size() + ": " + dataRow);

            for (Map<String, Object> step : steps) {
                String stepName = (String) step.get("name");
                String method = ((String) step.get("method")).toUpperCase();
                String url = (String) step.get("url");
                if (url == null && step.get("path") != null) {
                    url = baseUrl + step.get("path").toString();
                }
                Map<String, String> generatedValues = new HashMap<>();
                Map<String, Object> stepHeaders = (Map<String, Object>) step.get("headers");
                Map<String, Object> headers = resolveHeaders(defaultHeaders, stepHeaders, generatedValues);
                String body = null;

                // If body is external file
                if (step.get("bodyFile") != null) {
                    String bodyFile = (String) step.get("bodyFile");
                    body = JsonUtils.readJsonFile(bodyFile);
                } else if (step.get("body_file") != null) {
                    String bodyFile = (String) step.get("body_file");
                    body = JsonUtils.readJsonFile(bodyFile);
                } else if (step.get("body") != null) {
                    Object bodyObj = step.get("body");
                    if (bodyObj instanceof Map || bodyObj instanceof List) {
                        body = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(bodyObj);
                    } else {
                        body = bodyObj.toString();
                    }
                }

                // Replace any ${variable} in URL, headers, body with runtime variables
                url = replaceVariables(url, generatedValues);
                if (body != null) body = replaceVariables(body, generatedValues);

                // Execute HTTP call
                long startNs = System.nanoTime();
                Response response = HttpExecutor.execute(url, method, headers, body);
                long durationMs = (System.nanoTime() - startNs) / 1_000_000;
                boolean passed = response.getStatusCode() < 400;

                // Capture variables
                if (step.get("capture") != null) {
                    Map<String, String> captureMap = (Map<String, String>) step.get("capture");
                    for (String key : captureMap.keySet()) {
                        String jsonPath = captureMap.get(key);
                        if (jsonPath != null && jsonPath.startsWith("response.")) {
                            jsonPath = jsonPath.substring("response.".length());
                        }
                        Object value = JsonUtils.extractJsonValue(response, jsonPath);
                        runtimeVariables.put(key, value);
                        System.out.println("Captured: " + key + " = " + value);
                    }
                }

                // Validate schema if provided
                if (step.get("expectedSchema") != null) {
                    Map<String, Object> schema = (Map<String, Object>) step.get("expectedSchema");
                    try {
                        SchemaValidator.validate(schema, response);
                        System.out.println("✔ Passed: " + stepName);
                    } catch (Exception e) {
                        passed = false;
                        System.err.println("❌ Failed: " + stepName + " - " + e.getMessage());
                    }
                }

                // Validate full JSON Schema file if provided
                String schemaFile = (String) (step.get("schema_file") != null
                        ? step.get("schema_file")
                        : step.get("schemaFile"));
                if (schemaFile != null) {
                    schemaFile = replaceVariables(schemaFile, generatedValues);
                    try {
                        SchemaValidator.validateSchemaFile(schemaFile, response);
                        System.out.println("✔ Passed JSON Schema: " + stepName + " -> " + schemaFile);
                    } catch (Exception e) {
                        passed = false;
                        System.err.println("❌ Failed JSON Schema: " + stepName + " - " + e.getMessage());
                    }
                }

                // Add step to report
                ReportGenerator.addStep(
                        stepName,
                        method,
                        url,
                        headers,
                        body,
                        response.getStatusCode(),
                        response.getBody().asString(),
                        new HashMap<>(runtimeVariables), // copy
                        passed,
                        durationMs
                );
            }
        }
    }

    private static Map<String, Object> resolveHeaders(Map<String, Object> defaultHeaders,
                                                      Map<String, Object> stepHeaders,
                                                      Map<String, String> generatedValues) {
        if ((defaultHeaders == null || defaultHeaders.isEmpty()) && (stepHeaders == null || stepHeaders.isEmpty())) {
            return null;
        }

        Map<String, Object> resolved = new LinkedHashMap<>();
        if (defaultHeaders != null) {
            defaultHeaders.forEach((k, v) -> resolved.put(k, v == null ? null : replaceVariables(v.toString(), generatedValues)));
        }
        if (stepHeaders != null) {
            stepHeaders.forEach((k, v) -> resolved.put(k, v == null ? null : replaceVariables(v.toString(), generatedValues)));
        }
        return resolved;
    }

    private static List<Map<String, Object>> loadDataRows(String dataFile) throws Exception {
        if (dataFile == null || dataFile.isBlank()) {
            return Collections.singletonList(Collections.emptyMap());
        }

        Path dataPath = JsonUtils.resolvePath(dataFile);
        String content = Files.readString(dataPath);
        String lowerPath = dataFile.toLowerCase();

        if (lowerPath.endsWith(".json")) {
            Object parsed = OBJECT_MAPPER.readValue(content, Object.class);
            if (parsed instanceof List) {
                return (List<Map<String, Object>>) parsed;
            }
            if (parsed instanceof Map) {
                return Collections.singletonList((Map<String, Object>) parsed);
            }
            throw new IllegalArgumentException("JSON data_file must be an object or array of objects: " + dataFile);
        }

        if (lowerPath.endsWith(".yml") || lowerPath.endsWith(".yaml")) {
            Yaml yaml = new Yaml();
            Object parsed = yaml.load(content);
            if (parsed instanceof List) {
                return (List<Map<String, Object>>) parsed;
            }
            if (parsed instanceof Map) {
                return Collections.singletonList((Map<String, Object>) parsed);
            }
            throw new IllegalArgumentException("YAML data_file must be a map or list of maps: " + dataFile);
        }

        if (lowerPath.endsWith(".csv")) {
            return parseCsvRows(content);
        }

        throw new IllegalArgumentException("Unsupported data_file extension: " + dataFile + ". Use .json, .yaml, .yml, or .csv");
    }

    private static List<Map<String, Object>> parseCsvRows(String csvContent) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> lines = csvContent.lines().toList();
        if (lines.isEmpty()) {
            return Collections.singletonList(Collections.emptyMap());
        }

        List<String> headers = null;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (headers == null) {
                headers = parseCsvLine(line);
                continue;
            }

            List<String> values = parseCsvLine(line);
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String value = i < values.size() ? values.get(i) : "";
                row.put(headers.get(i), value);
            }
            rows.add(row);
        }

        if (rows.isEmpty()) {
            return Collections.singletonList(Collections.emptyMap());
        }
        return rows;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }

    // Replace ${var} placeholders with runtime variable values
    private static String replaceVariables(String input) {
        return replaceVariables(input, new HashMap<>());
    }

    private static String replaceVariables(String input, Map<String, String> generatedValues) {
        if (input == null) return null;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = resolvePlaceholderValue(key, generatedValues);
            if (replacement == null) {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String resolvePlaceholderValue(String key, Map<String, String> generatedValues) {
        Object runtimeValue = runtimeVariables.get(key);
        if (runtimeValue != null) {
            return runtimeValue.toString();
        }

        if (generatedValues.containsKey(key)) {
            return generatedValues.get(key);
        }

        String generated = switch (key) {
            case "uuid" -> UUID.randomUUID().toString();
            case "timestamp" -> String.valueOf(System.currentTimeMillis());
            case "timestampSeconds" -> String.valueOf(System.currentTimeMillis() / 1000);
            case "randomInt" -> String.valueOf(ThreadLocalRandom.current().nextInt(0, 1_000_000));
            default -> generateRangedRandomInt(key);
        };

        if (generated != null) {
            generatedValues.put(key, generated);
        }
        return generated;
    }

    private static String generateRangedRandomInt(String key) {
        if (!key.startsWith("randomInt(") || !key.endsWith(")")) {
            return null;
        }

        String args = key.substring("randomInt(".length(), key.length() - 1);
        String[] parts = args.split(",");
        if (parts.length != 2) {
            return null;
        }

        try {
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            if (max < min) {
                return null;
            }
            return String.valueOf(ThreadLocalRandom.current().nextInt(min, max + 1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
