package engine;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class CurlToYamlConverter {
    private static final Pattern LINE_CONTINUATION = Pattern.compile("\\\\\r?\n");

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java engine.CurlToYamlConverter <inputDir> [outputDir]");
            System.exit(1);
        }

        Path inputDir = Paths.get(args[0]);
        Path outputDir = args.length >= 2
                ? Paths.get(args[1])
                : Paths.get("src/main/resources/tests/generated");

        if (!Files.exists(inputDir) || !Files.isDirectory(inputDir)) {
            throw new IllegalArgumentException("Input directory not found: " + inputDir);
        }

        Files.createDirectories(outputDir);

        List<Path> curlFiles = Files.list(inputDir)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".curl"))
                .sorted()
                .toList();

        if (curlFiles.isEmpty()) {
            System.out.println("No .curl files found in: " + inputDir);
            return;
        }

        int converted = 0;
        for (Path curlFile : curlFiles) {
            String content = Files.readString(curlFile);
            CurlRequest request = parseCurl(content);
            Path outputFile = outputDir.resolve(toSafeName(curlFile.getFileName().toString()) + ".yaml");
            writeYaml(outputFile, request);
            converted++;
            System.out.println("Converted: " + curlFile + " -> " + outputFile);
        }

        System.out.println("Done. Total converted: " + converted);
    }

    private static CurlRequest parseCurl(String rawContent) {
        String normalized = LINE_CONTINUATION.matcher(rawContent).replaceAll(" ")
                .trim();
        List<String> tokens = tokenize(normalized);

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Empty curl content");
        }

        int cursor = 0;
        if ("curl".equalsIgnoreCase(tokens.get(0))) {
            cursor = 1;
        }

        String method = null;
        String url = null;
        String body = null;
        Map<String, Object> headers = new LinkedHashMap<>();

        for (int i = cursor; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if (("-X".equals(token) || "--request".equals(token)) && i + 1 < tokens.size()) {
                method = tokens.get(++i).toUpperCase(Locale.ROOT);
                continue;
            }

            if (("--url".equals(token)) && i + 1 < tokens.size()) {
                url = tokens.get(++i);
                continue;
            }

            if (("-H".equals(token) || "--header".equals(token)) && i + 1 < tokens.size()) {
                String header = tokens.get(++i);
                int idx = header.indexOf(':');
                if (idx > 0) {
                    String key = header.substring(0, idx).trim();
                    String value = header.substring(idx + 1).trim();
                    headers.put(key, value);
                }
                continue;
            }

            if (("-d".equals(token)
                    || "--data".equals(token)
                    || "--data-raw".equals(token)
                    || "--data-binary".equals(token)
                    || "--data-urlencode".equals(token)) && i + 1 < tokens.size()) {
                String part = tokens.get(++i);
                if (body == null) {
                    body = part;
                } else {
                    body = body + "&" + part;
                }
                continue;
            }

            if (token.startsWith("http://") || token.startsWith("https://")) {
                url = token;
            }
        }

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Could not find URL in curl command");
        }

        if (method == null || method.isBlank()) {
            method = (body != null && !body.isBlank()) ? "POST" : "GET";
        }

        return new CurlRequest(method, url, headers, body);
    }

    private static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inSingle) {
                if (c == '\'') {
                    inSingle = false;
                } else {
                    current.append(c);
                }
                continue;
            }

            if (inDouble) {
                if (c == '"' && !isEscaped(input, i)) {
                    inDouble = false;
                } else if (c == '\\' && i + 1 < input.length()) {
                    char next = input.charAt(i + 1);
                    if (next == '"' || next == '\\' || next == '$') {
                        current.append(next);
                        i++;
                    } else {
                        current.append(c);
                    }
                } else {
                    current.append(c);
                }
                continue;
            }

            if (Character.isWhitespace(c)) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            if (c == '\'') {
                inSingle = true;
                continue;
            }

            if (c == '"') {
                inDouble = true;
                continue;
            }

            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (Character.isWhitespace(next) || next == '"' || next == '\'' || next == '\\') {
                    current.append(next);
                    i++;
                    continue;
                }
            }

            current.append(c);
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static boolean isEscaped(String input, int quoteIndex) {
        int backslashes = 0;
        for (int i = quoteIndex - 1; i >= 0 && input.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return (backslashes % 2) == 1;
    }

    private static void writeYaml(Path outputFile, CurlRequest request) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Map<String, Object>> steps = new ArrayList<>();
        Map<String, Object> step = new LinkedHashMap<>();

        String stepName = toSafeName(outputFile.getFileName().toString());
        step.put("name", stepName);
        step.put("method", request.method);
        step.put("url", request.url);
        if (!request.headers.isEmpty()) {
            step.put("headers", request.headers);
        }
        if (request.body != null && !request.body.isBlank()) {
            step.put("body", request.body);
        }

        steps.add(step);
        root.put("steps", steps);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        Yaml yaml = new Yaml(options);

        Files.writeString(outputFile, yaml.dump(root));
    }

    private static String toSafeName(String fileName) {
        String base = fileName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        return base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private record CurlRequest(String method, String url, Map<String, Object> headers, String body) {
    }
}
