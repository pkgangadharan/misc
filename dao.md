Here are three examples of how to achieve this using different technologies commonly used in Java ecosystems:

1.  **Spring JDBC (JdbcTemplate)** - Recommended for most Spring applications.
2.  **Raw JDBC** - Best for standalone applications without frameworks.
3.  **JPA / Hibernate** - Best if you are using an ORM.

### Option 1: Spring JDBC (Recommended)
This is the cleanest approach if you are using Spring. `JdbcTemplate` handles the connection opening/closing and exception handling automatically.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Generic method to run any count query with optional parameters.
     * 
     * @param sql The SQL query (e.g., "SELECT COUNT(*) FROM users WHERE status = ?")
     * @param args Variable arguments matching the '?' placeholders in SQL
     * @return The count as a long
     */
    public long getCount(String sql, Object... args) {
        // queryForObject executes the query and converts the result to the generic class passed (Long)
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        
        // Handle potential null return (though COUNT usually returns 0, not null)
        return count != null ? count : 0;
    }

    // Example of a specific wrapper method using the generic one
    public long countActiveUsersInRegion(String region) {
        String sql = "SELECT COUNT(*) FROM users WHERE is_active = 1 AND region = ?";
        return getCount(sql, region);
    }
}
```

### Option 2: Raw JDBC (Standard Java)
Use this if you are not using any frameworks like Spring or Hibernate. It uses `try-with-resources` to ensure connections are closed.

```java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

public class GenericDao {

    private final DataSource dataSource;

    public GenericDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Executes a count query using raw JDBC.
     *
     * @param sql    The SQL query
     * @param params An array of parameters to bind to the statement
     * @return The count value
     */
    public long runCountQuery(String sql, Object... params) {
        long count = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind parameters if they exist
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    // JDBC index starts at 1, Array index starts at 0
                    ps.setObject(i + 1, params[i]);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getLong(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Log this in a real application
            throw new RuntimeException("Database error occurred", e);
        }

        return count;
    }
}
```

### Option 3: JPA / Hibernate (EntityManager)
If you are using Jakarta EE or Spring Data JPA with custom implementations.

```java
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsDao {

    @PersistenceContext
    private EntityManager entityManager;

    public long getCount(String jpql, Object... params) {
        Query query = entityManager.createQuery(jpql);

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                // Determine if using positional (?1) or named parameters. 
                // This example assumes positional logic for simplicity (1-based)
                query.setParameter(i + 1, params[i]);
            }
        }

        // executeQuery returns Object, cast to Long
        return (Long) query.getSingleResult();
    }
}
```

### Usage Example (Based on Option 1)

Here is how you would call the method from a Service or Main class:

```java
public class App {
    public static void main(String[] args) {
        // Assuming dao is injected
        UserDao dao = new UserDao(); 

        // 1. Count all users (No parameters)
        long totalUsers = dao.getCount("SELECT COUNT(*) FROM users");
        System.out.println("Total: " + totalUsers);

        // 2. Count users with parameters (Active users in 'US')
        String sql = "SELECT COUNT(*) FROM users WHERE active = ? AND country_code = ?";
        long usActiveUsers = dao.getCount(sql, true, "US");
        System.out.println("Active US Users: " + usActiveUsers);
    }
}
```
