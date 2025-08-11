import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import java.util.Collections;

/**
 * This class demonstrates how to connect to a MongoDB instance using the legacy
 * Java driver (pre-4.x). It specifically shows how to create a MongoClient
 * with authentication credentials and custom timeout settings.
 *
 * Note on Driver Versions:
 * The classes `com.mongodb.MongoClient` and `com.mongodb.DB` are part of the
 * legacy MongoDB Java driver (e.g., version 3.12.x). The modern driver (4.x and later)
 * uses `com.mongodb.client.MongoClient` and `com.mongodb.client.MongoDatabase`.
 * This example uses the legacy classes as requested.
 *
 * Maven Dependency for this code:
 * <dependency>
 * <groupId>org.mongodb</groupId>
 * <artifactId>mongo-java-driver</artifactId>
 * <version>3.12.11</version>
 * </dependency>
 */
public class LegacyMongoConnectionExample {

    public static void main(String[] args) {
        // --- Connection Parameters ---
        String host = "localhost";
        int port = 27017;
        String username = "your_username";
        String password = "your_password";
        // The database where the user is defined, typically 'admin'.
        String authDatabase = "admin";
        // The database you want to interact with after connecting.
        String targetDatabaseName = "your_target_db";

        MongoClient mongoClient = null;

        try {
            // --- 1. Configure Timeouts ---
            // Use MongoClientOptions.Builder to specify connection settings.
            MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder();

            // Connection timeout (e.g., 10 seconds). How long to wait for a connection to be established.
            optionsBuilder.connectTimeout(10000);

            // Socket timeout (e.g., 15 seconds). How long a send or receive on a socket can take.
            optionsBuilder.socketTimeout(15000);

            // Server selection timeout (e.g., 20 seconds). How long to wait for a server to become available.
            optionsBuilder.serverSelectionTimeout(20000);

            MongoClientOptions options = optionsBuilder.build();

            // --- 2. Create Authentication Credentials ---
            // This creates a credential object for the user.
            // It specifies the username, the database where the user is authenticated, and the password.
            MongoCredential credential = MongoCredential.createCredential(
                username,
                authDatabase,
                password.toCharArray()
            );

            // --- 3. Create the MongoClient ---
            // This brings all the pieces together:
            // - ServerAddress: The host and port of the MongoDB instance.
            // - Credentials: A list of credentials to use for authentication.
            // - Options: The custom timeout settings.
            mongoClient = new MongoClient(
                new ServerAddress(host, port),
                Collections.singletonList(credential),
                options
            );

            System.out.println("Successfully created MongoClient with authentication!");
            System.out.println("Connected to server: " + mongoClient.getAddress());

            // --- 4. Get the DB Object ---
            // Use the connected client to get an instance of the DB class for your target database.
            // This does not perform a network operation yet; it's a handle to the database.
            DB database = mongoClient.getDB(targetDatabaseName);

            System.out.println("Successfully got a handle for database: " + database.getName());
            System.out.println("Collections in this DB: " + database.getCollectionNames());


        } catch (Exception e) {
            // Handle exceptions, such as authentication failure or connection errors.
            System.err.println("An error occurred while connecting to MongoDB:");
            e.printStackTrace();
        } finally {
            // --- 5. Close the Connection ---
            // It's crucial to close the client to release resources.
            if (mongoClient != null) {
                mongoClient.close();
                System.out.println("MongoClient connection closed.");
            }
        }
    }
}
