package engine;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportGenerator {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final class ReportStep {
        private final String testFile;
        private final String stepName;
        private final String method;
        private final String url;
        private final Map<String, Object> requestHeaders;
        private final String requestBody;
        private final int statusCode;
        private final String responseBody;
        private final Map<String, Object> capturedVars;
        private final boolean passed;
        private final long durationMs;

        private ReportStep(String testFile, String stepName, String method, String url,
                           Map<String, Object> requestHeaders, String requestBody,
                           int statusCode, String responseBody, Map<String, Object> capturedVars,
                           boolean passed, long durationMs) {
            this.testFile = testFile;
            this.stepName = stepName;
            this.method = method;
            this.url = url;
            this.requestHeaders = requestHeaders;
            this.requestBody = requestBody;
            this.statusCode = statusCode;
            this.responseBody = responseBody;
            this.capturedVars = capturedVars;
            this.passed = passed;
            this.durationMs = durationMs;
        }
    }

    private static final List<ReportStep> reportSteps = new ArrayList<>();
    private static String currentTestFile = "";

    public static void startTestFile(String testFile) {
        currentTestFile = testFile;
    }

    public static void addStep(String stepName, String method, String url,
                               Map<String, Object> requestHeaders, String requestBody,
                               int statusCode, String responseBody, Map<String, Object> capturedVars,
                               boolean passed, long durationMs) {
        reportSteps.add(new ReportStep(
                currentTestFile,
                stepName,
                method,
                url,
                requestHeaders,
                requestBody,
                statusCode,
                responseBody,
                capturedVars,
                passed,
                durationMs
        ));
    }

    public static void generateReport(String filename) {
        try (FileWriter fw = new FileWriter(filename)) {
            fw.write(buildHtml());
            System.out.println("✅ Test report generated: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String buildHtml() {
        int passCount = 0;
        for (ReportStep step : reportSteps) {
            if (step.passed) passCount++;
        }
        int failCount = reportSteps.size() - passCount;

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        html.append("<title>Test Report</title>");
        html.append("<style>");
        html.append("body{font-family:Verdana,Arial,sans-serif;background:#f7f9fc;color:#111827;margin:0;padding:20px;}");
        html.append(".wrap{max-width:1200px;margin:0 auto;}");
        html.append(".head{display:flex;justify-content:space-between;align-items:flex-start;gap:12px;flex-wrap:wrap;margin-bottom:16px;}");
        html.append(".title{font-size:24px;font-weight:700;margin:0;}");
        html.append(".meta{color:#4b5563;font-size:13px;}");
        html.append(".chip{display:inline-block;border-radius:999px;padding:4px 10px;font-size:12px;font-weight:600;margin-right:8px;}");
        html.append(".ok{background:#dcfce7;color:#166534;}");
        html.append(".bad{background:#fee2e2;color:#991b1b;}");
        html.append(".table-wrap{overflow-x:auto;}");
        html.append("table{width:100%;table-layout:auto;border-collapse:collapse;background:#fff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;}");
        html.append("th,td{padding:10px 12px;border-bottom:1px solid #eef2f7;font-size:13px;vertical-align:top;}");
        html.append("th{background:#f8fafc;color:#334155;text-align:left;font-weight:600;}");
        html.append("tr:hover{background:#fafcff;}");
        html.append(".mono{font-family:Verdana,Arial,sans-serif;word-break:break-word;}");
        html.append(".small{font-size:12px;color:#475569;}");
        html.append(".status{font-weight:700;}");
        html.append(".status.ok{color:#166534;background:none;}");
        html.append(".status.bad{color:#991b1b;background:none;}");
        html.append(".panel{margin:0;}");
        html.append(".panel summary{cursor:pointer;color:#1d4ed8;font-weight:600;list-style-position:inside;}");
        html.append(".panel[open] summary{margin-bottom:8px;}");
        html.append("pre{white-space:pre-wrap;word-break:break-word;background:#f8fafc;border:1px solid #e5e7eb;border-radius:8px;padding:8px;margin:0;font-family:Consolas,'Courier New',monospace;line-height:1.4;}");
        html.append("</style></head><body><div class=\"wrap\">");

        html.append("<div class=\"head\">");
        html.append("<div><h1 class=\"title\">API Test Report</h1>");
        html.append("<div class=\"meta\">One line per request with expandable details</div></div>");
        html.append("<div>");
        html.append("<span class=\"chip ok\">Passed: ").append(passCount).append("</span>");
        html.append("<span class=\"chip bad\">Failed: ").append(failCount).append("</span>");
        html.append("<span class=\"chip\">Total: ").append(reportSteps.size()).append("</span>");
        html.append("</div></div>");

        html.append("<div class=\"table-wrap\"><table><colgroup>");
        html.append("<col style=\"width:4%\"><col style=\"width:16%\"><col style=\"width:10%\"><col style=\"width:28%\"><col style=\"width:28%\"><col style=\"width:7%\"><col style=\"width:7%\">");
        html.append("</colgroup><thead><tr>");
        html.append("<th>#</th><th>Test Case</th><th>Step</th><th>Request</th><th>Response</th><th>Time</th><th>Result</th>");
        html.append("</tr></thead><tbody>");

        int index = 1;
        for (ReportStep step : reportSteps) {
            String prettyHeaders = prettyText(asJsonOrDash(step.requestHeaders));
            String prettyRequestBody = prettyText(stringOrDash(step.requestBody));
            String prettyResponseBody = prettyText(stringOrDash(step.responseBody));

            html.append("<tr>");
            html.append("<td>").append(index++).append("</td>");
            html.append("<td class=\"mono\">").append(escapeHtml(toTestCaseName(step.testFile))).append("</td>");
            html.append("<td>").append(escapeHtml(step.stepName)).append("</td>");
            html.append("<td class=\"mono\">");
            html.append("<details class=\"panel\">");
            html.append("<summary>").append(escapeHtml(step.method)).append(" ").append(escapeHtml(step.url)).append("</summary>");
            html.append("<div class=\"small\">Headers</div><pre>").append(escapeHtml(prettyHeaders)).append("</pre>");
            html.append("<div class=\"small\" style=\"margin-top:6px;\">Body</div><pre>").append(escapeHtml(prettyRequestBody)).append("</pre>");
            html.append("</details>");
            html.append("</td>");
            html.append("<td class=\"mono\">");
            html.append("<details class=\"panel\">");
            html.append("<summary>Status ").append(step.statusCode).append("</summary>");
            html.append("<div class=\"small\">Body</div><pre>").append(escapeHtml(prettyResponseBody)).append("</pre>");
            html.append("</details>");
            html.append("</td>");
            html.append("<td>").append(step.durationMs).append(" ms</td>");
            html.append("<td><span class=\"status ").append(step.passed ? "ok" : "bad").append("\">")
                    .append(step.passed ? "PASS" : "FAIL").append("</span>");

            if (step.capturedVars != null && !step.capturedVars.isEmpty()) {
                html.append("<details class=\"panel\"><summary>Captured</summary><pre>")
                        .append(escapeHtml(prettyText(asJsonOrDash(step.capturedVars)))).append("</pre></details>");
            }
            html.append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody></table></div></div></body></html>");
        return html.toString();
    }

    private static String stringOrDash(Object value) {
        return value == null ? "-" : value.toString();
    }

    private static String asJsonOrDash(Object value) {
        if (value == null) {
            return "-";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ignored) {
            return value.toString();
        }
    }

    private static String prettyText(String value) {
        if (value == null || value.isBlank() || "-".equals(value)) {
            return "-";
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(OBJECT_MAPPER.readTree(value));
        } catch (Exception ignored) {
            return value;
        }
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String toTestCaseName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "-";
        }

        String suffix = "";
        int rowIdx = raw.indexOf(" [row ");
        String pathPart = raw;
        if (rowIdx >= 0) {
            pathPart = raw.substring(0, rowIdx);
            suffix = raw.substring(rowIdx);
        }

        String normalized = pathPart.replace("\\", "/");
        int slash = normalized.lastIndexOf('/');
        String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;

        return base + suffix;
    }
}
