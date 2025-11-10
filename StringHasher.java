import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A utility class to create and validate one-way hashes of strings
 * using the SHA-256 algorithm.
 *
 * This version uses MessageDigest.isEqual for a constant-time comparison
 * to protect against timing attacks.
 */
public class StringHasher {

    private static final String ALGORITHM = "SHA-256";

    /**
     * Hashes a given string using SHA-256 and returns the raw bytes.
     *
     * @param input The original string.
     * @return The byte array of the SHA-256 hash.
     * @throws NoSuchAlgorithmException If SHA-256 is not available.
     */
    private byte[] hash(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(ALGORITHM);
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encodes (hashes) a given string using SHA-256.
     *
     * @param input The original string.
     * @return A 64-character hex string representing the SHA-256 hash,
     * or null if the input was null.
     */
    public String encode(String input) {
        if (input == null) {
            return null;
        }
        try {
            byte[] hashBytes = hash(input);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // This should never happen with a standard algorithm like SHA-256
            throw new RuntimeException("Could not find hashing algorithm: " + ALGORITHM, e);
        }
    }

    /**
     * Validates if an original string matches a given encoded (hashed) string
     * using a constant-time comparison.
     *
     * @param originalString The plaintext string to test.
     * @param encodedString  The SHA-256 hex hash to compare against.
     * @return true if the original string's hash matches the encoded string,
     * false otherwise.
     */
    public boolean validate(String originalString, String encodedString) {
        if (originalString == null || encodedString == null) {
            return false;
        }
        
        try {
            // 1. Hash the original string to get its byte[] representation
            byte[] originalHashBytes = hash(originalString);
            
            // 2. Convert the provided hex string back into its byte[] representation
            byte[] encodedHashBytes = hexToBytes(encodedString);
            
            // 3. Use MessageDigest.isEqual for a constant-time comparison
            // This prevents timing attacks.
            return MessageDigest.isEqual(originalHashBytes, encodedHashBytes);
            
        } catch (NoSuchAlgorithmException e) {
            // This should never happen with SHA-256
            throw new RuntimeException("Could not find hashing algorithm: " + ALGORITHM, e);
        } catch (IllegalArgumentException e) {
            // This can happen if encodedString is not valid hex 
            // (e.g., odd length, invalid characters)
            return false;
        }
    }

    /**
     * Helper method to convert a byte array into its hexadecimal
     * string representation.
     */
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Helper method to convert a hexadecimal string into a byte array.
     *
     * @param hex The hexadecimal string.
     * @return The corresponding byte array.
     * @throws IllegalArgumentException if the hex string has an odd length
     * or contains invalid characters.
     */
    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even number of characters.");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            // Parse two hex characters at a time
            int firstDigit = Character.digit(hex.charAt(i), 16);
            int secondDigit = Character.digit(hex.charAt(i + 1), 16);

            if (firstDigit == -1 || secondDigit == -1) {
                throw new IllegalArgumentException("Invalid hex character in string.");
            }
            
            data[i / 2] = (byte) ((firstDigit << 4) + secondDigit);
        }
        return data;
    }
}
