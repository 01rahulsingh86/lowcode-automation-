package engine;

import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NLTriggerRunner {
    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            System.err.println("Usage: java engine.NLTriggerRunner [--config <path>] <natural language prompt>");
            System.exit(1);
        }

        String configPath = "nl_triggers.yaml";
        int promptStart = 0;
        if ("--config".equals(args[0])) {
            if (args.length < 3) {
                throw new IllegalArgumentException("Usage: --config <path> <natural language prompt>");
            }
            configPath = args[1];
            promptStart = 2;
        }

        String prompt = String.join(" ", java.util.Arrays.copyOfRange(args, promptStart, args.length)).trim();
        if (prompt.isBlank()) {
            throw new IllegalArgumentException("Natural language prompt cannot be empty");
        }

        List<TriggerRule> rules = loadRules(configPath);
        MatchResult matchResult = matchPrompt(prompt, rules);

        if (matchResult.matchedRules.isEmpty()) {
            throw new IllegalArgumentException("No trigger matched prompt: " + prompt);
        }

        String matchedNames = matchResult.matchedRules.stream()
                .map(r -> r.name)
                .collect(Collectors.joining(", "));

        System.out.println("Prompt: " + prompt);
        System.out.println("Matched trigger(s): " + matchedNames);
        System.out.println("Running flows:");
        for (String file : matchResult.flowFiles) {
            System.out.println(" - " + file);
        }

        TestEngine.main(matchResult.flowFiles.toArray(new String[0]));
    }

    @SuppressWarnings("unchecked")
    private static List<TriggerRule> loadRules(String configPath) throws Exception {
        Path resolved = JsonUtils.resolvePath(configPath);
        Object loaded = new Yaml().load(Files.readString(resolved));

        List<Map<String, Object>> rawRules;
        if (loaded instanceof Map) {
            Object triggers = ((Map<String, Object>) loaded).get("triggers");
            if (!(triggers instanceof List)) {
                throw new IllegalArgumentException("Config must include 'triggers' list: " + configPath);
            }
            rawRules = (List<Map<String, Object>>) triggers;
        } else if (loaded instanceof List) {
            rawRules = (List<Map<String, Object>>) loaded;
        } else {
            throw new IllegalArgumentException("Unsupported trigger config format: " + configPath);
        }

        List<TriggerRule> rules = new ArrayList<>();
        int idx = 1;
        for (Map<String, Object> raw : rawRules) {
            String name = raw.get("name") == null ? "trigger_" + idx : raw.get("name").toString();
            List<String> phrases = toStringList(raw.get("phrases"));
            List<String> keywordsAll = toStringList(raw.get("keywords_all") != null ? raw.get("keywords_all") : raw.get("keywordsAll"));
            List<String> keywordsAny = toStringList(raw.get("keywords_any") != null ? raw.get("keywords_any") : raw.get("keywordsAny"));
            List<String> files = toStringList(raw.get("files") != null ? raw.get("files") : raw.get("flows"));

            if (files.isEmpty()) {
                throw new IllegalArgumentException("Trigger '" + name + "' must include files/flows");
            }
            if (phrases.isEmpty() && keywordsAll.isEmpty() && keywordsAny.isEmpty()) {
                throw new IllegalArgumentException("Trigger '" + name + "' must include phrases and/or keywords");
            }

            rules.add(new TriggerRule(name, lowercaseList(phrases), lowercaseList(keywordsAll), lowercaseList(keywordsAny), files));
            idx++;
        }

        return rules;
    }

    private static MatchResult matchPrompt(String prompt, List<TriggerRule> rules) {
        String normalizedPrompt = normalize(prompt);
        Set<String> tokens = words(normalizedPrompt);

        List<TriggerRule> matched = new ArrayList<>();
        LinkedHashSet<String> flowFiles = new LinkedHashSet<>();

        for (TriggerRule rule : rules) {
            boolean phraseMatch = !rule.phrases.isEmpty() && rule.phrases.stream().anyMatch(normalizedPrompt::contains);
            boolean allMatch = !rule.keywordsAll.isEmpty() && rule.keywordsAll.stream().allMatch(tokens::contains);
            boolean anyMatch = !rule.keywordsAny.isEmpty() && rule.keywordsAny.stream().anyMatch(tokens::contains);

            if (phraseMatch || allMatch || anyMatch) {
                matched.add(rule);
                flowFiles.addAll(rule.files);
            }
        }

        return new MatchResult(matched, new ArrayList<>(flowFiles));
    }

    private static Set<String> words(String text) {
        Set<String> out = new LinkedHashSet<>();
        for (String part : text.split("[^a-z0-9_]+")) {
            if (!part.isBlank()) {
                out.add(part);
            }
        }
        return out;
    }

    private static String normalize(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static List<String> lowercaseList(List<String> in) {
        List<String> out = new ArrayList<>();
        for (String v : in) {
            out.add(v.toLowerCase(Locale.ROOT).trim());
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object value) {
        if (value == null) return List.of();
        if (value instanceof List) {
            List<String> out = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                if (item != null) {
                    out.add(item.toString());
                }
            }
            return out;
        }
        return List.of(value.toString());
    }

    private record TriggerRule(String name,
                               List<String> phrases,
                               List<String> keywordsAll,
                               List<String> keywordsAny,
                               List<String> files) {
    }

    private record MatchResult(List<TriggerRule> matchedRules, List<String> flowFiles) {
    }
}
