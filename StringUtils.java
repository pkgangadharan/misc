import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A utility class for string manipulation.
 */
public final class StringUtils {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private StringUtils() {}

    /**
     * Truncates a string to a maximum number of bytes using the common UTF-8 charset.
     * This method ensures that multi-byte characters are not split in the middle.
     *
     * @param input    The string to truncate. Can be null or empty.
     * @param maxBytes The maximum number of bytes the resulting string can have.
     * @return The truncated string. Returns an empty string if input is null or maxBytes <= 0.
     */
    public static String truncateToByteLength(final String input, final int maxBytes) {
        return truncateToByteLength(input, maxBytes, StandardCharsets.UTF_8);
    }

    /**
     * Truncates a string to a maximum number of bytes using a specified charset.
     * This method is safe for multi-byte characters (like emojis) and will not cut them in half.
     *
     * @param input    The string to truncate. Can be null or empty.
     * @param maxBytes The maximum number of bytes the resulting string can have.
     * @param charset  The charset to use for calculating the byte length (e.g., StandardCharsets.UTF_8).
     * @return The truncated string, or the original string if it's already within the
     * limit. Returns an empty string if input is null or maxBytes <= 0.
     */
    public static String truncateToByteLength(final String input, final int maxBytes, final Charset charset) {
        // 1. Handle edge cases
        if (input == null || input.isEmpty() || maxBytes <= 0) {
            return "";
        }

        // 2. Get the byte array of the original string
        final byte[] inputBytes = input.getBytes(charset);

        // 3. If it's already within the limit, return the original string
        if (inputBytes.length <= maxBytes) {
            return input;
        }

        // 4. Iterate by code points to find the correct cut-off index
        int currentBytes = 0;
        int endIndex = 0; // This will be the exclusive end index for substring

        for (int i = 0; i < input.length(); ) {
            // Get the current code point (handles surrogate pairs for complex chars)
            final int codePoint = input.codePointAt(i);
            
            // Determine its byte length in the specified charset
            final int codePointByteLength = new String(Character.toChars(codePoint)).getBytes(charset).length;

            // If this character would push us over the limit, stop
            if (currentBytes + codePointByteLength > maxBytes) {
                break;
            }

            currentBytes += codePointByteLength;
            
            // Advance index by 1 or 2, depending on whether it's a surrogate pair
            i += Character.charCount(codePoint);
            endIndex = i;
        }

        // 5. Return the substring up to the safe end index
        return input.substring(0, endIndex);
    }


    /**
     * Main method for demonstration purposes.
     */
    public static void main(String[] args) {
        // Example 1: Simple ASCII string
        String simple = "Hello, World"; // 12 chars, 12 bytes in UTF-8
        System.out.println("--- Simple ASCII ---");
        System.out.printf("Original: '%s' (%d bytes)\n", simple, simple.getBytes().length);
        System.out.printf("Cut to 8 bytes: '%s'\n\n", truncateToByteLength(simple, 8));

        // Example 2: String with multi-byte CJK characters
        // Each Chinese char is 3 bytes in UTF-8
        String multiByte = "你好,世界"; // 5 chars, 13 bytes in UTF-8
        System.out.println("--- Multi-Byte (CJK) ---");
        System.out.printf("Original: '%s' (%d bytes)\n", multiByte, multiByte.getBytes().length);
        System.out.printf("Cut to 7 bytes: '%s'\n", truncateToByteLength(multiByte, 7)); // Should be "你好,"
        System.out.printf("Cut to 8 bytes: '%s'\n\n", truncateToByteLength(multiByte, 8)); // Still "你好," as next char is 3 bytes

        // Example 3: String with a 4-byte emoji character
        String emoji = "Hello 😂"; // 7 "chars", 10 bytes in UTF-8 (😂 is 4 bytes)
        System.out.println("--- Multi-Byte (Emoji) ---");
        System.out.printf("Original: '%s' (%d bytes)\n", emoji, emoji.getBytes().length);
        System.out.printf("Cut to 6 bytes: '%s'\n", truncateToByteLength(emoji, 6)); // "Hello "
        System.out.printf("Cut to 9 bytes: '%s'\n", truncateToByteLength(emoji, 9)); // Still "Hello "
        System.out.printf("Cut to 10 bytes: '%s'\n\n", truncateToByteLength(emoji, 10)); // "Hello 😂"
    }
}
