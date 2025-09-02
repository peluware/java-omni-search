package com.peluware.omnisearch.jpa;

import com.peluware.omnisearch.core.OmniSearchOptions;
import com.peluware.omnisearch.jpa.entities.*;
import cz.jirutka.rsql.parser.RSQLParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaOmniSearchTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
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
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "true");

        return Persistence.createEntityManagerFactory("test-pu", properties);
    }

    // Test data setup methods
    private void setupTestData() {
        // Test data
        var alice = createUser("Alice", "alice@example.com", true, User.Level.HIGH, Set.of(User.Role.USER, User.Role.ADMIN));
        alice.setContacts(Set.of(
                createContact("Contact1", "Last1"),
                createContact("Contact2", "Last2")
        ));

        var bob = createUser("Bob", "bob@example.com", false, User.Level.MEDIUM, Set.of(User.Role.GUEST));

        var dave = createUser("Dave", "charlie@example.net", true, User.Level.LOW, Set.of(User.Role.USER));

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
            List<User> result = omniSearch.search(User.class, options);

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
            List<User> result = omniSearch.search(User.class, options);

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
            List<User> result = omniSearch.search(User.class, options);

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
            List<User> result = omniSearch.search(User.class, options);

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
            List<User> result = omniSearch.search(User.class, options);

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
            List<User> result = omniSearch.search(User.class, options);

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
            var conditions = new RSQLParser().parse("name==alice;email==*example.com*");
            var options = new OmniSearchOptions().query(conditions);

            // When
            List<User> result = omniSearch.search(User.class, options);

            // Then
            assertEquals(1, result.size());
            assertEquals("Alice", result.getFirst().getName());
            assertTrue(result.getFirst().getEmail().contains("example.com"));
        }

        @Test
        @DisplayName("Should filter by roles using RSQL IN operator")
        void testSearchFilterRoleIn() {
            // Given
            var conditions = new RSQLParser().parse("roles=in=ADMIN");
            var options = new OmniSearchOptions().query(conditions);

            // When
            List<User> result = omniSearch.search(User.class, options);

            // Then
            assertEquals(1, result.size());
            assertEquals("Alice", result.getFirst().getName());
            assertTrue(result.getFirst().getRoles().contains(User.Role.USER));
            assertTrue(result.getFirst().getRoles().contains(User.Role.ADMIN));
        }

    }

    @Nested
    @DisplayName("Complex Relationship Tests")
    class ComplexRelationshipTests {

        @Test
        @DisplayName("Should search through nested relationships - houses > country > continent")
        void testSearchWithNestedRelationships() {
            // Given
            setupComplexTestData();
            omniSearch = new JpaOmniSearch(em);

            var query = new RSQLParser().parse("houses.country.countinent.code==NA");
            var options = new OmniSearchOptions().query(query);

            // When
            List<User> result = omniSearch.search(User.class, options);

            // Then
            assertEquals(2, result.size());
        }

        private void setupComplexTestData() {
            em.getTransaction().begin();

            // Create continents
            var northAmerica = createContinent("NA", "North America", "North American continent");
            var southAmerica = createContinent("SA", "South America", "South American continent");

            em.persist(northAmerica);
            em.persist(southAmerica);

            // Create countries
            var usa = createCountry("US", "United States", northAmerica);
            var canada = createCountry("CA", "Canada", southAmerica);
            var mexico = createCountry("MX", "Mexico", southAmerica);

            em.persist(usa);
            em.persist(canada);
            em.persist(mexico);

            // Create users with houses
            var eve = createUserWithHouses("Eve", "eve@ovi.com", User.Level.HIGH,
                    List.of(
                            createHouse("House1", "123 Main St", 3, usa),
                            createHouse("House2", "456 Elm St", 4, canada),
                            createHouse("House3", "789 Oak St", 5, mexico)
                    ));

            var frank = createUserWithHouses("Frank", "frank@ovi.com", User.Level.MEDIUM,
                    List.of(
                            createHouse("House4", "321 Pine St", 2, usa),
                            createHouse("House5", "654 Maple St", 3, canada)
                    ));

            em.persist(eve);
            em.persist(frank);
            em.getTransaction().commit();
        }

        private Countinent createContinent(String code, String name, String description) {
            return Countinent.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .build();
        }

        private Country createCountry(String code, String name, Countinent continent) {
            return Country.builder()
                    .code(code)
                    .name(name)
                    .countinent(continent)
                    .build();
        }

        private User createUserWithHouses(String name, String email, User.Level level, List<House> houses) {
            return User.builder()
                    .name(name)
                    .email(email)
                    .active(true)
                    .level(level)
                    .roles(Set.of(User.Role.USER))
                    .houses(houses)
                    .build();
        }

        private House createHouse(String name, String address, int numberOfRooms, Country country) {
            return House.builder()
                    .name(name)
                    .address(address)
                    .numberOfRooms(numberOfRooms)
                    .country(country)
                    .build();
        }
    }
}