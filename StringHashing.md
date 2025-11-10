Based on your requirements, the "best method" is not "encoding" but cryptographic hashing.
Here’s a breakdown of why:
 * Encoding (like Base64) is a two-way process. It changes data into a different format (e.g., to safely send binary data over text-based systems) and is always meant to be decoded back to the original.
 * Hashing (like SHA-256) is a one-way process. It takes an input of any size and produces a short, fixed-size string (a "hash").
   * Cannot be decoded: It's computationally impossible to reverse the process and get the original string from the hash.
   * Unique: Different inputs will produce different hashes. Even a tiny change (like "hello" vs. "Hello") results in a completely different hash.
   * Short & Fixed-Size: SHA-256 always produces a 64-character hexadecimal string, regardless of whether the input is one word or an entire book.
The best, most common, and secure algorithm for this is SHA-256.
Java Hash Utility Class
Here is a Java class StringHasher that uses SHA-256 to "encode" (hash) a string and validate it.
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A utility class to create and validate one-way hashes of strings
 * using the SHA-256 algorithm.
 */
public class StringHasher {

    private static final String ALGORITHM = "SHA-256";

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
            // Get an instance of the SHA-256 message digest
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            
            // Calculate the hash. 
            // We must specify a charset (UTF-8 is standard) to ensure
            // the same string produces the same bytes on every system.
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Convert the byte array into a hexadecimal string
            return bytesToHex(hashBytes);
            
        } catch (NoSuchAlgorithmException e) {
            // This should never happen with a standard algorithm like SHA-256
            throw new RuntimeException("Could not find hashing algorithm: " + ALGORITHM, e);
        }
    }

    /**
     * Validates if an original string matches a given encoded (hashed) string.
     *
     * @param originalString The plaintext string to test.
     * @param encodedString  The SHA-256 hash to compare against.
     * @return true if the original string's hash matches the encoded string, 
     * false otherwise.
     */
    public boolean validate(String originalString, String encodedString) {
        if (originalString == null || encodedString == null) {
            return false;
        }
        
        // Hash the original string using the same method
        String newHash = encode(originalString);
        
        // Compare the new hash to the provided hash.
        // See the important security note below.
        return newHash.equals(encodedString);
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
}

How to Use the Class
Here is a simple example of how to use the StringHasher class.
public class HashExample {

    public static void main(String[] args) {
        StringHasher hasher = new StringHasher();
        
        String originalPassword = "MyS3cret!Password123";
        String differentPassword = "MyS3cret!Password456";

        // 1. Encoding (Hashing)
        // This is what you would store in your database
        String storedHash = hasher.encode(originalPassword);
        
        System.out.println("Original String: " + originalPassword);
        System.out.println("Encoded (Hashed): " + storedHash);
        System.out.println("Hash Length: " + storedHash.length()); // Always 64
        
        System.out.println("---");

        // 2. Validation (e.g., at login)
        
        // Check with the correct password
        boolean isCorrect = hasher.validate(originalPassword, storedHash);
        System.out.println("Login attempt with '" + originalPassword + "': " + isCorrect);
        
        // Check with the wrong password
        boolean isIncorrect = hasher.validate(differentPassword, storedHash);
        System.out.println("Login attempt with '" + differentPassword + "': " + isIncorrect);
    }
}

Example Output:
Original String: MyS3cret!Password123
Encoded (Hashed): a446b78b5e828a2a5a54a49c693302a2c1b7470f3f6120e82c5a2c2084b6f16c
Hash Length: 64
---
Login attempt with 'MyS3cret!Password123': true
Login attempt with 'MyS3cret!Password456': false

⚠️ Important Security Note on Validation
The validate method above uses newHash.equals(encodedString). For most uses, this is fine.
However, in high-security applications (like a web server handling password logins), this method is vulnerable to a "timing attack." A simple string comparison stops checking as soon as it finds a non-matching character. An attacker could theoretically measure the tiny differences in time it takes for the comparison to fail, allowing them to guess the hash one character at a time.
A more secure method is a "constant-time comparison," which takes the same amount of time regardless of where the mismatch occurs. The built-in Java way to do this is MessageDigest.isEqual(byte[] a, byte[] b).
To use it, you would need to modify the validate method to convert the encodedString (hex) back into a byte[] and compare it with the byte array of the newly generated hash. The simple string comparison is often sufficient, but it's crucial to be aware of this distinction.
