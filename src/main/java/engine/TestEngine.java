package engine;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestEngine {

    /**
     * Runs a single test YAML file
     * @param yamlFile Path to YAML file
     * @throws Exception
     */
    public static void runTestFile(String yamlFile) throws Exception {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(new FileInputStream(yamlFile));

        String baseUrl = (String) data.get("env");
        List<Map<String, Object>> steps = (List<Map<String, Object>>) data.get("steps");

        // Initialize context for this test file (used for ${var} chaining)
        Map<String, Object> context = new HashMap<>();
        HttpExecutor.setContext(context);

        System.out.println("\n--- Running Test File: " + yamlFile + " ---");

        for (Map<String, Object> step : steps) {
            System.out.println("\nExecuting Step: " + step.get("name"));
            HttpExecutor.execute(step, baseUrl);
        }
    }

    public static void main(String[] args) throws Exception {
        // Example: run multiple YAML test files
        runTestFile("tests/posts_test.yaml");
        runTestFile("tests/comments_test.yaml");
     //   runTestFile("tests/orders_test.yaml"); // you can keep adding more
    }
}
