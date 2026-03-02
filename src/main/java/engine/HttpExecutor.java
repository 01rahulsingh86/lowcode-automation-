package engine;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.Map;

public class HttpExecutor {

    /**
     * Execute HTTP request
     *
     * @param url     Full URL
     * @param method  GET/POST/PUT/DELETE
     * @param headers Map of headers
     * @param body    Request body as String (can be null)
     * @return Response object from RestAssured
     */
    public static Response execute(String url, String method, Map<String, Object> headers, String body) {
        io.restassured.specification.RequestSpecification req = RestAssured.given();

        // Add headers
        if (headers != null) {
            headers.forEach((k, v) -> req.header(k, v.toString()));
        }

        // Add body
        if (body != null && !body.isEmpty()) {
            req.body(body);
        }

        // Execute
        Response response;
        switch (method.toUpperCase()) {
            case "POST":
                response = req.post(url);
                break;
            case "PUT":
                response = req.put(url);
                break;
            case "DELETE":
                response = req.delete(url);
                break;
            case "GET":
            default:
                response = req.get(url);
        }

        // Log to console (optional)
        System.out.println("Response Status: " + response.getStatusCode());
        System.out.println("Response Body: " + response.getBody().asString());

        return response;
    }
}
