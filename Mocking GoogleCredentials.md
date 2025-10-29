Mocking `GoogleCredentials.fromStream` is tricky for two main reasons:

1.  `GoogleCredentials` is a `final` class.
2.  `fromStream` is a `static` method.

Standard mocking frameworks like Mockito cannot mock `final` classes or `static` methods by default. However, you can solve this using **Mockito's inline mock maker**.

Here are the best ways to handle this, from the most direct answer to the recommended long-term solution.

### Option 1: Mocking the Static Method with `mockito-inline` (Direct Answer)

This approach directly mocks the `static` `fromStream` call. It requires adding the `mockito-inline` dependency to your project.

**1. Add the Dependency (Maven):**
Make sure your `pom.xml` includes `mockito-inline`. If you're using `mockito-core` version 3.4.0 or later, this is often included, but it's good to be explicit.

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <version>5.2.0</version> <scope>test</scope>
</dependency>
```

**2. Create the Mock in Your Test:**
You use `Mockito.mockStatic()` in a `try-with-resources` block. This block defines the scope where the static method is mocked.

Here is a complete JUnit 5 example:

```java
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.InputStream;
import java.util.Date;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

// This is the class you are trying to test
class MyService {
    public String initialize() {
        try (InputStream is = /* ... getting your stream ... */ mock(InputStream.class)) {
            // This is the line we want to mock
            GoogleCredentials credentials = GoogleCredentials.fromStream(is);
            
            // The service then uses the credentials
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

// This is your test class
class MyServiceTest {

    @Test
    void testInitialize() throws Exception {
        // 1. Since GoogleCredentials is final, you must use mockito-inline.
        //    Create a mock for the GoogleCredentials *instance*.
        GoogleCredentials mockCredentials = mock(GoogleCredentials.class);
        
        // 2. Create a fake AccessToken to return
        AccessToken fakeToken = new AccessToken("fake-token-123", new Date(System.currentTimeMillis() + 3600_000));
        
        // 3. Stub the behavior of the mock instance
        when(mockCredentials.getAccessToken()).thenReturn(fakeToken);
        // You can also stub other methods like refreshIfExpired()
        doNothing().when(mockCredentials).refreshIfExpired();

        // 4. Use mockStatic in a try-with-resources block
        try (MockedStatic<GoogleCredentials> mockedStatic = mockStatic(GoogleCredentials.class)) {
            
            // 5. Define the static mock behavior
            mockedStatic.when(() -> GoogleCredentials.fromStream(any(InputStream.class)))
                        .thenReturn(mockCredentials);

            // 6. Run your code under test
            MyService myService = new MyService();
            String tokenValue = myService.initialize();

            // 7. Assert and Verify
            assertEquals("fake-token-123", tokenValue);

            // 8. (Optional) Verify the static method was called
            mockedStatic.verify(() -> GoogleCredentials.fromStream(any(InputStream.class)));
            
            // 9. (Optional) Verify methods were called on the instance
            verify(mockCredentials).refreshIfExpired();
            verify(mockCredentials).getAccessToken();
        }
    }
}
```

-----

### Option 2: `mockStatic` to Return a *Real* (but Fake) Credential

This is a variation of Option 1 and is often simpler if you **don't need to verify methods** called on the `GoogleCredentials` object (like `refresh()`).

Instead of mocking the `GoogleCredentials` class, you create a *real*, simple, in-memory credential object and have `fromStream` return that.

```java
@Test
void testInitialize_SimpleMock() {
    
    // 1. Create a real, lightweight credentials object with a fake token
    AccessToken fakeToken = new AccessToken("fake-token-123", new Date(System.currentTimeMillis() + 3600_000));
    GoogleCredentials fakeCredentials = GoogleCredentials.create(fakeToken);

    // 2. Use mockStatic to intercept the fromStream call
    try (MockedStatic<GoogleCredentials> mockedStatic = mockStatic(GoogleCredentials.class)) {
        
        // 3. Return your fake (but real) object instead
        mockedStatic.when(() -> GoogleCredentials.fromStream(any(InputStream.class)))
                    .thenReturn(fakeCredentials);

        // 4. Run your code under test
        MyService myService = new MyService();
        String tokenValue = myService.initialize(); // This will now use your fakeCredentials

        // 5. Assert
        assertEquals("fake-token-123", tokenValue);
    }
}
```

This approach is cleaner as it avoids mocking a `final` class entirely. You only mock the `static` method.

-----

### Option 3: The Recommended Long-Term Solution (Refactoring)

Mocking static methods is often a sign that your code is tightly coupled. A better, more testable design is to use **Dependency Injection**.

Instead of having `MyService` *create* its own `GoogleCredentials`, you *pass* them in.

**1. Refactor Your Service:**

**Before:**

```java
class MyService {
    public void initialize() {
        // Tightly coupled: MyService is responsible for creating credentials
        try (InputStream is = /* ... */) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(is);
            // ... use credentials
        } catch (Exception e) { /* ... */ }
    }
}
```

**After (Refactored):**

```java
class MyService {
    private final GoogleCredentials credentials;

    // Inject the dependency via the constructor
    public MyService(GoogleCredentials credentials) {
        this.credentials = credentials;
    }

    public void initialize() {
        // Now it just uses the credentials
        try {
            credentials.refreshIfExpired();
            // ... use credentials
        } catch (Exception e) { /* ... */ }
    }
}
```

  * Your *real* application code (e.g., your Spring `@Configuration` or main method) will be responsible for loading the stream and creating the `GoogleCredentials` object *once* and passing it to `MyService`.

**2. Your Test Becomes Much Simpler:**

Now your test doesn't need to mock any `static` methods.

```java
@Test
void testMyService_Refactored() throws Exception {
    
    // 1. Mock the dependency (still need mockito-inline for the final class)
    GoogleCredentials mockCredentials = mock(GoogleCredentials.class);
    AccessToken fakeToken = new AccessToken("fake-token-123", new Date(System.currentTimeMillis() + 3600_000));

    // 2. Stub its behavior
    when(mockCredentials.getAccessToken()).thenReturn(fakeToken);
    doNothing().when(mockCredentials).refreshIfExpired();

    // 3. Inject the mock
    MyService myService = new MyService(mockCredentials);

    // 4. Run the code
    myService.initialize();

    // 5. Verify the mock was used
    verify(mockCredentials).refreshIfExpired();
    verify(mockCredentials).getAccessToken();
}
```

### Summary

| Approach | How it Works | Pros | Cons |
| :--- | :--- | :--- | :--- |
| **Option 1** | `mockStatic` + `mock(GoogleCredentials.class)` | Directly tests your existing code. Full control over the mock. | Requires `mockito-inline`. More complex test setup. |
| **Option 2** | `mockStatic` + `GoogleCredentials.create(...)` | Simpler than Option 1. No need to mock the `final` class itself. | Can't verify calls to `refresh()`, etc. |
| **Option 3** | **Refactor + Dependency Injection** | **Recommended.** Results in cleaner, decoupled code (SOLID). Test is very simple. | Requires you to change your application code. |
