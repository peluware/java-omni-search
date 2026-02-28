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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JpaComplexRelationshipTests {

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
        properties.put("hibernate.format_sql", "false");

        return Persistence.createEntityManagerFactory("test-pu", properties);
    }

    @BeforeEach
    void setUp() {
        em = emf.createEntityManager();
        em.getTransaction().begin();

        setupComplexTestData();

        em.getTransaction().commit();
        omniSearch = new JpaOmniSearch(em);
    }

    @AfterEach
    void tearDown() {
        cleanupComplexTestData();
        em.close();
    }

    @AfterAll
    void close() {
        emf.close();
    }

    @Test
    @DisplayName("Should search through nested relationships - houses > country > continent")
    void testSearchWithNestedRelationships() {
        var query = "houses.country.countinent.code==NA";
        var options = new OmniSearchOptions().query(query);

        // When
        List<User> result = omniSearch.list(User.class, options);

        // Then
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should sort by nested relationship field - contacts.firstName ascending")
    void testSortByHouseCountryNameAscending() {
        var options = new OmniSearchOptions()
                .query("name=in=(Eve,Frank)")
                .sort(com.peluware.domain.Sort.by(com.peluware.domain.Order.ascending("contacts.firstName")));

        // When
        List<User> result = omniSearch.list(User.class, options);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.get(0).getContacts().stream().anyMatch(c -> "Jane".equals(c.getFirstName())));
        assertTrue(result.get(1).getContacts().stream().anyMatch(c -> "John".equals(c.getFirstName())));
    }

    private void setupComplexTestData() {
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
        eve.setContacts(Set.of(Contacts.builder().firstName("John").lastName("Doe").build()));

        var frank = createUserWithHouses("Frank", "frank@ovi.com", User.Level.MEDIUM,
                List.of(
                        createHouse("House4", "321 Pine St", 2, usa),
                        createHouse("House5", "654 Maple St", 3, canada)
                ));
        frank.setContacts(Set.of(Contacts.builder().firstName("Jane").lastName("Smith").build()));

        em.persist(eve);
        em.persist(frank);
    }

    private void cleanupComplexTestData() {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM House").executeUpdate();
        em.createQuery("DELETE FROM Contacts").executeUpdate();
        em.createQuery("DELETE FROM User").executeUpdate();
        em.createQuery("DELETE FROM Country").executeUpdate();
        em.createQuery("DELETE FROM Countinent").executeUpdate();
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
