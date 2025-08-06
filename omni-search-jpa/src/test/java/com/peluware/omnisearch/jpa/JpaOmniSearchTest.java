package com.peluware.omnisearch.jpa;

import com.peluware.omnisearch.core.OmniSearchOptions;
import com.peluware.omnisearch.jpa.entities.*;
import cz.jirutka.rsql.parser.RSQLParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaOmniSearchTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private JpaOmniSearch omniSearch;

    @BeforeAll
    void init() {
        emf = Persistence.createEntityManagerFactory("test-pu");
    }

    @BeforeEach
    void setUp() {
        em = emf.createEntityManager();
        em.getTransaction().begin();

        em.persist(User.builder()
                .name("Alice")
                .email("alice@example.com")
                .active(true)
                .level(User.Level.HIGH)
                .roles(Set.of(User.Role.USER, User.Role.ADMIN))
                .contacts(Set.of(
                        Contacts.builder()
                                .firstName("Contact1")
                                .lastName("Last1")
                                .build(),
                        Contacts.builder()
                                .firstName("Contact2")
                                .lastName("Last2")
                                .build()
                ))
                .build()
        );

        em.persist(User.builder()
                .name("Bob")
                .email("bob@example.com")
                .active(false)
                .level(User.Level.MEDIUM)
                .roles(Set.of(User.Role.GUEST))
                .build()
        );

        em.persist(User.builder()
                .name("Dave")
                .email("charlie@example.net")
                .active(true)
                .level(User.Level.LOW)
                .roles(Set.of(User.Role.USER))
                .build()
        );

        em.getTransaction().commit();

        omniSearch = new JpaOmniSearch(em);
    }


    @AfterEach
    void tearDown() {

        em.getTransaction().begin();
        var users = em.createQuery("SELECT u FROM User u", User.class).getResultList();
        for (User user : users) {
            em.remove(user);
        }
        em.getTransaction().commit();
        em.close();
    }

    @AfterAll
    void close() {
        emf.close();
    }

    @Test
    void testSearchByName() {

        var options = new OmniSearchOptions()
                .search("Alice");

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(1, result.size());
        assertEquals("Alice", result.getFirst().getName());
    }

    @Test
    void testSearchByEmailDomain() {

        var options = new OmniSearchOptions()
                .search("example.com");

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(2, result.size());
    }

    @Test
    void testSearchBooleanTrue() {

        var options = new OmniSearchOptions()
                .search("true");

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(2, result.size());
    }

    @Test
    void testSearchEnumLevel() {

        var options = new OmniSearchOptions()
                .search("HIGH");

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(1, result.size());
        assertEquals(User.Level.HIGH, result.getFirst().getLevel());
    }

    @Test
    void testSearchContactFirstName() {

        var options = new OmniSearchOptions()
                .search("Contact1")
                .joins("contacts");

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().getContacts().stream().anyMatch(contact -> "Contact1".equals(contact.getFirstName())));
    }


    @Test
    void testSearchByRole() {

        var options = new OmniSearchOptions()
                .search("USER");

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(user -> user.getRoles().contains(User.Role.USER)));
    }

    @Test
    void testSearchFilterNameAndEmail() {

        var conditions = new RSQLParser().parse("name==alice;email==*example.com*");
        var options = new OmniSearchOptions()
                .query(conditions);

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(1, result.size());
        assertEquals("Alice", result.getFirst().getName());
        assertTrue(result.getFirst().getEmail().contains("example.com"));
    }

    @Test
    void testSearchWithHouses() {
        em.getTransaction().begin();
        var countinentNA = Countinent.builder()
                .code("NA")
                .name("North America")
                .description("North American continent")
                .build();

        var countinentSA = Countinent.builder()
                .code("SA")
                .name("South America")
                .description("South American continent")
                .build();

        em.persist(countinentNA);
        em.persist(countinentSA);

        var countryUS = Country.builder().code("US").name("United States").countinent(countinentNA).build();
        var countryCA = Country.builder().code("CA").name("Canada").countinent(countinentSA).build();
        var countryMX = Country.builder().code("MX").name("Mexico").countinent(countinentSA).build();

        em.persist(countryUS);
        em.persist(countryCA);
        em.persist(countryMX);

        var user1 = User.builder()
                .name("Eve")
                .email("eve@ovi.com")
                .active(true)
                .level(User.Level.HIGH)
                .roles(Set.of(User.Role.USER))
                .houses(List.of(
                        House.builder()
                                .name("House1")
                                .address("123 Main St")
                                .numberOfRooms(3)
                                .country(countryUS)
                                .build(),
                        House.builder()
                                .name("House2")
                                .address("456 Elm St")
                                .numberOfRooms(4)
                                .country(countryCA)
                                .build(),
                        House.builder()
                                .name("House3")
                                .address("789 Oak St")
                                .numberOfRooms(5)
                                .country(countryMX)
                                .build()
                ))
                .build();

        var user2 = User.builder()
                .name("Frank")
                .email("frank@ovi.com")
                .active(true)
                .level(User.Level.MEDIUM)
                .roles(Set.of(User.Role.USER))
                .houses(List.of(
                        House.builder()
                                .name("House4")
                                .address("321 Pine St")
                                .numberOfRooms(2)
                                .country(countryUS)
                                .build(),
                        House.builder()
                                .name("House5")
                                .address("654 Maple St")
                                .numberOfRooms(3)
                                .country(countryCA)
                                .build()
                ))
                .build();

        em.persist(user1);
        em.persist(user2);
        em.getTransaction().commit();
        omniSearch = new JpaOmniSearch(em);


        var conditions = new RSQLParser().parse("houses.country.countinent.code==NA");
        var options = new OmniSearchOptions()
                .query(conditions);

        List<User> result = omniSearch.search(User.class, options);
        assertEquals(2, result.size());
    }

}
