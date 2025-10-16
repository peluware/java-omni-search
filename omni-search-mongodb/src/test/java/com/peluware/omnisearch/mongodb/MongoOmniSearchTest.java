package com.peluware.omnisearch.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.peluware.domain.Order;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.omnisearch.OmniSearchOptions;

import cz.jirutka.rsql.parser.RSQLParser;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.time.Year;
import java.util.*;
import java.util.stream.Stream;

import static org.bson.codecs.configuration.CodecRegistries.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class MongoOmniSearchTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoOmniSearch omniSearch;

    // Test entities
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {

        private ObjectId id;
        private String name;
        private String description;
        private Double price;
        private Integer stock;
        private Boolean active;
        private Year releaseYear;
        private Category category;
        private List<String> tags = new ArrayList<>();
        private List<Integer> ratings = new ArrayList<>();
        private List<Review> reviews = new ArrayList<>();

        public Product(String name, String description, Double price, Integer stock, Boolean active) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.stock = stock;
            this.active = active;
            this.tags = new ArrayList<>();
            this.ratings = new ArrayList<>();
            this.reviews = new ArrayList<>();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category {
        private String name;
        private String code;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Review {
        private String author;
        private String comment;
        private Integer rating;
    }

    @BeforeEach
    void setUp() {
        var pojoCodecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromCodecs(new YearCodec()),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        mongoClient = MongoClients.create(
                MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(mongoDBContainer.getConnectionString()))
                        .codecRegistry(pojoCodecRegistry)
                        .build()
        );
        database = mongoClient.getDatabase("test_omnisearch");

        // Clear any existing data
        database.getCollection("product").deleteMany(Filters.empty());

        omniSearch = new MongoOmniSearch(database);

        // Insert test data
        insertTestData();
    }

    @AfterEach
    void tearDown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    private void insertTestData() {
        var collection = database.getCollection("product", Product.class);

        // Create test products
        var laptop = new Product("Gaming Laptop", "High-performance laptop for gaming", 1299.99, 5, true);
        laptop.setReleaseYear(Year.of(2023));
        laptop.setCategory(new Category("Electronics", "ELEC"));
        laptop.setTags(List.of("gaming", "laptop", "high-performance"));
        laptop.setRatings(List.of(5, 4, 5, 4));
        laptop.setReviews(List.of(
                new Review("John", "Great laptop!", 5),
                new Review("Jane", "Good performance", 4)
        ));

        var mouse = new Product("Wireless Mouse", "Ergonomic wireless mouse", 29.99, 50, true);
        mouse.setReleaseYear(Year.of(2022));
        mouse.setCategory(new Category("Electronics", "ELEC"));
        mouse.setTags(List.of("mouse", "wireless", "ergonomic"));
        mouse.setRatings(List.of(4, 4, 3, 5));

        var keyboard = new Product("Mechanical Keyboard", "RGB mechanical keyboard", 89.99, 20, true);
        keyboard.setReleaseYear(Year.of(2023));
        keyboard.setCategory(new Category("Electronics", "ELEC"));
        keyboard.setTags(List.of("keyboard", "mechanical", "rgb"));
        keyboard.setRatings(List.of(5, 5, 4));

        var chair = new Product("Office Chair", "Comfortable office chair", 199.99, 10, false);
        chair.setReleaseYear(Year.of(2021));
        chair.setCategory(new Category("Furniture", "FURN"));
        chair.setTags(List.of("chair", "office", "comfortable"));
        chair.setRatings(List.of(3, 4, 4));

        var monitor = new Product("4K Monitor", "Ultra HD 4K monitor", 349.99, 8, true);
        monitor.setReleaseYear(Year.of(2023));
        monitor.setCategory(new Category("Electronics", "ELEC"));
        monitor.setTags(List.of("monitor", "4k", "ultra-hd"));
        monitor.setRatings(List.of(5, 5, 5, 4));

        collection.insertMany(List.of(laptop, mouse, keyboard, chair, monitor));
    }


    @ParameterizedTest
    @DisplayName("Should search and return expected product")
    @MethodSource("searchTestData")
    void testSearchWithExpectedResults(String searchTerm, String expectedProductName, String testDescription) {

        var options = new OmniSearchOptions()
                .search(searchTerm);

        var results = omniSearch.list(Product.class, options);

        assertEquals(1, results.size(), "Should find exactly one result for: " + testDescription);
        assertEquals(expectedProductName, results.getFirst().getName(), "Should find correct product for: " + testDescription);
    }

    static Stream<Arguments> searchTestData() {
        return Stream.of(
                // Should search by string field (name)
                Arguments.of("laptop", "Gaming Laptop", "Should search by string field (name)"),
                // Should search by string field case-insensitive
                Arguments.of("WIRELESS", "Wireless Mouse", "Should search by string field case insensitive"),
                // Should search by numeric field (price)
                Arguments.of("29.99", "Wireless Mouse", "Should search by numeric field (price)"),
                // Should search by integer field (stock)
                Arguments.of("50", "Wireless Mouse", "Should search by integer field (stock)"),
                // Should search by boolean field
                Arguments.of("false", "Office Chair", "Should search by boolean field"),
                // Should search by Year field
                Arguments.of("2022", "Wireless Mouse", "Should search by Year field"),
                // Should search in string arrays/lists
                Arguments.of("gaming", "Gaming Laptop", "Should search in string arrays/lists")
        );
    }

    @Test
    @DisplayName("Should search in integer arrays/lists")
    void testSearchInIntegerArray() {
        var options = new OmniSearchOptions()
                .search("5");

        var results = omniSearch.list(Product.class, options);

        // Should find products that have rating 5 or stock 5
        assertTrue(results.size() >= 2);
        var names = results.stream().map(Product::getName).toList();
        assertTrue(names.contains("Gaming Laptop")); // has rating 5 and stock 5
    }

    @Test
    @DisplayName("Should return empty results for non-matching search")
    void testNoMatchingResults() {
        var options = new OmniSearchOptions()
                .search("nonexistent");

        var results = omniSearch.list(Product.class, options);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should apply sorting")
    void testSorting() {
        var options = new OmniSearchOptions()
                .sort(Sort.by("price", Order.Direction.ASC));

        var results = omniSearch.list(Product.class, options);

        assertEquals(5, results.size());
        // Should be sorted by price ascending
        assertTrue(results.get(0).getPrice() <= results.get(1).getPrice());
        assertTrue(results.get(1).getPrice() <= results.get(2).getPrice());
    }

    @Test
    @DisplayName("Should apply sorting descending")
    void testSortingDescending() {
        var options = new OmniSearchOptions()
                .sort(Sort.by("price", Order.Direction.DESC));

        var results = omniSearch.list(Product.class, options);

        assertEquals(5, results.size());
        // Should be sorted by price descending
        assertTrue(results.get(0).getPrice() >= results.get(1).getPrice());
        assertEquals("Gaming Laptop", results.get(0).getName()); // Most expensive
    }

    @Test
    @DisplayName("Should apply pagination")
    void testPagination() {
        var options = new OmniSearchOptions()
                .sort(Sort.by("name", Order.Direction.ASC))
                .pagination(Pagination.of(0, 2));

        var results = omniSearch.list(Product.class, options);

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Should apply pagination with offset")
    void testPaginationWithOffset() {
        var options = new OmniSearchOptions()
                .sort(Sort.by("name", Order.Direction.ASC))
                .pagination(Pagination.of(2, 2));

        var results = omniSearch.list(Product.class, options);

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Should combine search with sorting and pagination")
    void testSearchWithSortingAndPagination() {
        var options = new OmniSearchOptions()
                .search("Electronics") // This won't match any direct field
                .sort(Sort.by("price", Order.Direction.DESC))
                .pagination(Pagination.of(0, 10));

        var results = omniSearch.list(Product.class, options);

        // Should return empty since "Electronics" doesn't match basic fields
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should search with propagations (complex fields)")
    void testSearchWithPropagations() {
        var options = new OmniSearchOptions()
                .search("Electronics")
                .propagations(Set.of("category"));

        var results = omniSearch.list(Product.class, options);

        // Should find all electronics products (4 products)
        assertEquals(4, results.size());
        var names = results.stream().map(Product::getName).toList();
        assertTrue(names.contains("Gaming Laptop"));
        assertTrue(names.contains("Wireless Mouse"));
        assertTrue(names.contains("Mechanical Keyboard"));
        assertTrue(names.contains("4K Monitor"));
    }

    @Test
    @DisplayName("Should count documents correctly")
    void testCount() {
        var options = new OmniSearchOptions()
                .search("mouse");

        long count = omniSearch.count(Product.class, options);

        assertEquals(1, count);
    }

    @Test
    @DisplayName("Should count all documents when no search criteria")
    void testCountAll() {
        var options = new OmniSearchOptions();

        long count = omniSearch.count(Product.class, options);

        assertEquals(5, count);
    }

    @Test
    @DisplayName("Should handle empty search string")
    void testEmptySearch() {
        var options = new OmniSearchOptions()
                .search("");

        var results = omniSearch.list(Product.class, options);

        assertEquals(5, results.size()); // Should return all products
    }

    @Test
    @DisplayName("Should handle null search string")
    void testNullSearch() {
        var options = new OmniSearchOptions()
                .search(null);

        var results = omniSearch.list(Product.class, options);

        assertEquals(5, results.size()); // Should return all products
    }

    @Test
    @DisplayName("Should search using RSQL query (price > 100)")
    void testWithRsqlPriceGt100() {
        var options = new OmniSearchOptions()
                .query(new RSQLParser().parse("price>100"));

        var results = omniSearch.list(Product.class, options);

        assertEquals(3, results.size()); // Should return 3 products with price > 100
        var names = results.stream().map(Product::getName).toList();
        assertTrue(names.contains("Gaming Laptop"));
        assertTrue(names.contains("Office Chair"));
        assertTrue(names.contains("4K Monitor"));
    }

    @Test
    @DisplayName("Should search using RSQL query (price == 89.99)")
    void testWithRsqlPriceEq89_99() {
        var options = new OmniSearchOptions()
                .query(new RSQLParser().parse("price==89.99"));

        var results = omniSearch.list(Product.class, options);

        assertEquals(1, results.size()); // Should return 1 product with price == 89.99
        assertEquals("Mechanical Keyboard", results.getFirst().getName());
    }
}