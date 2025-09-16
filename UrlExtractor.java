import java.net.MalformedURLException;
import java.net.URL;

public class UrlExtractor {

    /**
     * Extracts the protocol and host from a given HTTP URL string.
     *
     * @param urlString The full URL string (e.g., "https://www.example.com/home/").
     * @return A string containing the scheme and host (e.g., "https://www.example.com"),
     * or null if the input URL is malformed.
     */
    public static String getSchemeAndHost(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            return null;
        }
        try {
            // Create a URL object from the string
            URL url = new URL(urlString);
            
            // Reconstruct the part you need using getProtocol() and getHost()
            return url.getProtocol() + "://" + url.getHost();
        } catch (MalformedURLException e) {
            // Handle cases where the input string is not a valid URL
            System.err.println("Invalid URL format: " + e.getMessage());
            return null;
        }
    }

    // Main method to demonstrate the functionality
    public static void main(String[] args) {
        String exampleUrl1 = "https://www.example.com/home/";
        String exampleUrl2 = "http://api.myservice.org:8080/v1/users?id=123";
        String invalidUrl = "not-a-valid-url";

        System.out.println("Original URL: " + exampleUrl1);
        System.out.println("Extracted Part: " + getSchemeAndHost(exampleUrl1));
        // Expected output: https://www.example.com

        System.out.println("\nOriginal URL: " + exampleUrl2);
        System.out.println("Extracted Part: " + getSchemeAndHost(exampleUrl2));
        // Expected output: http://api.myservice.org
        
        System.out.println("\nOriginal URL: " + invalidUrl);
        System.out.println("Extracted Part: " + getSchemeAndHost(invalidUrl));
        // Expected output: Invalid URL format: no protocol: not-a-valid-url
        //                  Extracted Part: null
    }
}
