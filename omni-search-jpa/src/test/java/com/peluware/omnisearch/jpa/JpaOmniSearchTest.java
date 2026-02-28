package com.peluware.omnisearch.jpa;

import com.peluware.omnisearch.OmniSearchOptions;
import com.peluware.omnisearch.jpa.entities.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaOmniSearchTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15")
            .withDatabaseName("omnisearch_test")
            .withUsername("test")
            .withPassword("test");

    private EntityManagerFactory emf;
    private EntityManager em;
    private JpaOmniSearch omniSearch;

    @BeforeAll
    void init() {
        emf = createEntityManagerFactory();
    }

    @BeforeEach
    void setUp() {
        em = emf.createEntityManager();
        em.getTransaction().begin();

        setupTestData();

        em.getTransaction().commit();
        omniSearch = new JpaOmniSearch(em);
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
        em.close();
    }

    @AfterAll
    void close() {
        emf.close();
    }

    // Configuration methods
    private EntityManagerFactory createEntityManagerFactory() {
        Map<String, String> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        properties.put("jakarta.persistence.jdbc.url", postgres.getJdbcUrl());
        properties.put("jakarta.persistence.jdbc.user", postgres.getUsername());
        properties.put("jakarta.persistence.jdbc.password", postgres.getPassword());
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.show_sql", "true");
        properties.put("hibernate.format_sql", "false");

        return Persistence.createEntityManagerFactory("test-pu", properties);
    }

    // Test data setup methods
    private void setupTestData() {
        // Test data
        var alice = createUser("Alice",
                "alice@example.com",
                true,
                User.Level.HIGH,
                Set.of(User.Role.USER, User.Role.ADMIN)
        );
        alice.setContacts(Set.of(
                createContact("Contact1", "Last1"),
                createContact("Contact2", "Last2")
        ));

        var bob = createUser(
                "Bob",
                "bob@example.com",
                false,
                User.Level.MEDIUM,
                Set.of(User.Role.GUEST)
        );

        var dave = createUser(
                "Dave",
                "charlie@example.net",
                true,
                User.Level.LOW,
                Set.of(User.Role.USER)
        );

        em.persist(alice);
        em.persist(bob);
        em.persist(dave);
    }

    private User createUser(String name, String email, boolean active, User.Level level, Set<User.Role> roles) {
        return User.builder()
                .name(name)
                .email(email)
                .active(active)
                .level(level)
                .roles(roles)
                .build();
    }

    private Contacts createContact(String firstName, String lastName) {
        return Contacts.builder()
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }

    private void cleanupTestData() {
        em.getTransaction().begin();
        var users = em.createQuery("SELECT u FROM User u", User.class).getResultList();
        for (var user : users) {
            em.remove(user);
        }
        em.getTransaction().commit();
        em.close();
    }

    // Search tests
    @Nested
    @DisplayName("Text Search Tests")
    class TextSearchTests {

        @Test
        @DisplayName("Should find user by name")
        void testSearchByName() {
            // Given
            var options = new OmniSearchOptions().search("Alice");

            // When
            List<User> result = omniSearch.list(User.class, options);

            // Then
            assertEquals(1, result.size());
            assertEquals("Alice", result.getFirst().getName());
        }

        @Test
        @DisplayName("Should find users by email domain")
        void testSearchByEmailDomain() {
            // Given
            var options = new OmniSearchOptions().search("example.com");

            // When
            List<User> result = omniSearch.list(User.class, options);

            // Then
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(user -> user.getEmail().contains("example.com")));
        }

        @Test
        @DisplayName("Should find users with contact first name")
        void testSearchContactFirstName() {
            // Given
            var options = new OmniSearchOptions()
                    .search("Contact1")
                    .propagations("contacts");

            // When
            List<User> result = omniSearch.list(User.class, options);

            // Then
            assertEquals(1, result.size());
            assertTrue(result.getFirst().getContacts().stream().anyMatch(contact -> "Contact1".equals(contact.getFirstName())));
        }
    }

    @Nested
    @DisplayName("Boolean and Enum Search Tests")
    class BooleanAndEnumSearchTests {

        @Test
        @DisplayName("Should find active users by boolean search")
        void testSearchBooleanTrue() {
            // Given
            var options = new OmniSearchOptions().search("true");

            // When
            List<User> result = omniSearch.list(User.class, options);

            // Then
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(User::isActive));
        }

        @Test
        @DisplayName("Should find users by enum level")
        void testSearchEnumLevel() {
            // Given
            var options = new OmniSearchOptions().search("HIGH");

            // When
            List<User> result = omniSearch.list(User.class, options);

            // Then
            assertEquals(1, result.size());
            assertEquals(User.Level.HIGH, result.getFirst().getLevel());
        }

        @Test
        @DisplayName("Should find users by role")
        void testSearchByRole() {
            // Given
            var options = new OmniSearchOptions().search("USER");

            // When
            List<User> result = omniSearch.list(User.class, options);

            // Then
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(user -> user.getRoles().contains(User.Role.USER)));
        }
    }

    @Nested
    @DisplayName("RSQL Filter Tests")
    class RSQLFilterTests {

        @Test
        @DisplayName("Should filter by name and email using RSQL")
        void testSearchFilterNameAndEmail() {
            // Given
            var conditions = "name==alice;email==*example.com*";
            var options = new OmniSearchOptions().query(conditions);

            // When
            List<User> result = omniSearch.list(User.class, options);

            // Then
            assertEquals(1, result.size());
            assertEquals("Alice", result.getFirst().getName());
            assertTrue(result.getFirst().getEmail().contains("example.com"));
        }

        @Test
        @DisplayName("Should filter by roles using RSQL IN operator")
        void testSearchFilterRoleIn() {
            // Given
            var conditions = "roles=in=ADMIN";
            var options = new OmniSearchOptions().query(conditions);

            // When
            List<User> result = omniSearch.list(User.class, options);

            // Then
            assertEquals(1, result.size());
            assertEquals("Alice", result.getFirst().getName());
            assertTrue(result.getFirst().getRoles().contains(User.Role.USER));
            assertTrue(result.getFirst().getRoles().contains(User.Role.ADMIN));
        }

    }


    @Nested
    @DisplayName("Sort Tests")
    class SortTests {

        @Test
        @DisplayName("Should sort users by name ascending")
        void testSortByNameAscending() {
            // Given
            var options = new OmniSearchOptions()
                    .sort(com.peluware.domain.Sort.by(com.peluware.domain.Order.ascending("name")));

            // When
            List<User> result = omniSearch.list(User.class, options);

            // Then
            assertEquals(3, result.size());
            assertEquals("Alice", result.get(0).getName());
            assertEquals("Bob", result.get(1).getName());
            assertEquals("Dave", result.get(2).getName());
        }
    }
}