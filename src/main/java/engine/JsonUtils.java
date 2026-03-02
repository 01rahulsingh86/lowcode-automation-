package engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Yaml YAML = new Yaml();

    // Read request body from file. JSON is returned as-is; YAML is converted to JSON.
    public static String readJsonFile(String filePath) throws IOException {
        Path resolved = resolvePath(filePath);
        String content = Files.readString(resolved);
        String lowerPath = filePath.toLowerCase();

        if (lowerPath.endsWith(".yml") || lowerPath.endsWith(".yaml")) {
            Object yamlObject = YAML.load(content);
            try {
                return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(yamlObject);
            } catch (Exception e) {
                throw new IOException("Failed to convert YAML body file to JSON: " + filePath, e);
            }
        }

        return content;
    }

    public static Path resolvePath(String filePath) throws IOException {
        Path directPath = Paths.get(filePath);
        if (Files.exists(directPath)) {
            return directPath;
        }

        Path resourcesPath = Paths.get("src/main/resources", filePath);
        if (Files.exists(resourcesPath)) {
            return resourcesPath;
        }

        throw new IOException("File not found: " + filePath);
    }

    // Extract a value using JSONPath from a RestAssured Response
    public static Object extractJsonValue(Response response, String jsonPath) {
        if (response == null || response.getBody() == null) return null;
        return JsonPath.from(response.getBody().asString()).get(jsonPath);
    }
}
