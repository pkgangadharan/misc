Building a robust HTTP client in Java using the `java.net.http.HttpClient` (introduced in Java 11). It’s cleaner than the old `HttpURLConnection` and more modern than many external libraries.

Requirements for **pooling, keep-alive, retries, and pagination**, configure the client specifically and use a loop to handle the pages.

### The Implementation

Here is a production-ready approach. The `HttpClient` handles connection pooling and keep-alive internally when used as a singleton or long-lived instance.

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ApiClient {

    // The client is thread-safe and manages its own pool. 
    // Reusing this instance enables Keep-Alive and connection pooling.
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Fetches all pages from a paginated endpoint.
     * @param baseUrl The URL with a placeholder or suffix for pagination
     * @param token The M2M Bearer token
     */
    public List<String> fetchAllPages(String baseUrl, String token) {
        List<String> allData = new ArrayList<>();
        int currentPage = 1;
        boolean hasMoreData = true;

        while (hasMoreData) {
            String url = baseUrl + currentPage;
            String responseBody = fetchWithRetry(url, token);

            if (responseBody == null || responseBody.isBlank() || responseBody.equals("[]")) {
                hasMoreData = false;
            } else {
                allData.add(responseBody);
                currentPage++;
            }
        }
        return allData;
    }

    private String fetchWithRetry(String url, String token) {
        int maxAttempts = 2; // Initial try + 1 retry
        
        for (int i = 0; i < maxAttempts; i++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return response.body();
                } else {
                    System.err.println("Request failed with status: " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("Attempt " + (i + 1) + " failed: " + e.getMessage());
                if (i == maxAttempts - 1) throw new RuntimeException("Failed after retry", e);
            }
        }
        return null;
    }
}

```

---

### Key Features Explained

* **Connection Pooling & Keep-Alive:** By defining the `HttpClient` as a `static final` constant, you ensure the JVM reuses the same connection pool. The `HttpClient` manages a pool of TCP connections and keeps them alive for subsequent requests by default.
* **The Retry Logic:** The `fetchWithRetry` method wraps the request in a simple loop. If an `Exception` (like a timeout or network glitch) occurs, it catches it and tries one more time before failing.
* **Pagination Logic:** The `while` loop continues as long as `hasMoreData` is true. It specifically checks for `null`, empty strings, or an empty JSON array `[]`, which are common markers for the end of a dataset.
* **M2M Auth:** The token is passed via the `Authorization` header using the standard `Bearer` scheme.

### Considerations for Production

* **JSON Parsing:** This method returns a `List<String>`. In a real-world app, you'd likely want to use **Jackson** or **Gson** inside the loop to map the `response.body()` to a specific Java Object.
* **Backoff:** For the retry, it is often wise to add a small `Thread.sleep()` between attempts to give the server a moment to recover.

**Would you like me to show you how to integrate a JSON library like Jackson to parse these results into objects automatically?**
