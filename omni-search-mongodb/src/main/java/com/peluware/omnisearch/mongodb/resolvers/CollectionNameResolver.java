package com.peluware.omnisearch.mongodb.resolvers;

import com.peluware.omnisearch.mongodb.ReflectionUtils;
import lombok.experimental.UtilityClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for resolving MongoDB collection names from Java classes.
 * This resolver supports multiple annotation sources including Spring Data MongoDB,
 * Quarkus MongoDB with Panache, and Morphia ODM.
 *
 * <p>The default resolution order is:
 * <ol>
 *   <li>Spring Data MongoDB {@code @Document} annotation (if available)</li>
 *   <li>Quarkus {@code @MongoEntity} annotation (if available)</li>
 *   <li>Morphia {@code @Entity} annotation (if available)</li>
 *   <li>Class name converted to lowercase as fallback</li>
 * </ol>
 *
 * @author Peluware
 * @since 1.0
 */
@UtilityClass
@SuppressWarnings("java:S1192")
public final class CollectionNameResolver {

    private static final List<CollectionNameProvider> DEFAULT_PROVIDERS = new ArrayList<>(List.of(
            new SpringDocumentProvider(),
            new QuarkusMongoEntityProvider(),
            new MorphiaEntityProvider()
    ));

    private static final Map<Class<?>, String> COLLECTION_NAME_CACHE = new ConcurrentHashMap<>();


    /**
     * Resolves the MongoDB collection name for the given Java class using the default providers.
     * This method is thread-safe and can be called from multiple threads.
     *
     * @param clazz the Java class to resolve the collection name for
     * @return the resolved MongoDB collection name, never null
     */
    public static String resolveCollectionName(Class<?> clazz) {
        return COLLECTION_NAME_CACHE.computeIfAbsent(clazz, c -> {
            for (var provider : DEFAULT_PROVIDERS) {
                var result = provider.resolveCollectionName(clazz);
                if (result.isPresent()) {
                    return result.get();
                }
            }
            return clazz.getSimpleName().toLowerCase(); // Fallback to class name in lowercase
        });
    }

    /**
     * Adds a custom collection name provider to the resolver.
     * @param provider the custom provider to add
     */
    public static void addCollectionNameProvider(CollectionNameProvider provider) {
        if (provider != null) {
            DEFAULT_PROVIDERS.add(provider);
        }
    }

    /**
     * Adds a custom collection name provider at a specific index in the resolver.
     * @param index the index at which to insert the provider
     * @param provider the custom provider to add
     */
    public static void addCollectionNameProvider(int index, CollectionNameProvider provider) {
        if (provider != null) {
            DEFAULT_PROVIDERS.add(index, provider);
        }
    }

    /**
     * A provider interface for resolving collection names from specific annotation sources.
     * Implementations should return {@link Optional#empty()} if they cannot resolve
     * the collection name from their specific annotation source.
     */
    @FunctionalInterface
    public interface CollectionNameProvider {
        /**
         * Attempts to resolve the collection name for the given class.
         *
         * @param clazz the class to resolve the collection name for
         * @return an Optional containing the resolved name, or empty if this provider
         *         cannot resolve the name from its annotation source
         */
        Optional<String> resolveCollectionName(Class<?> clazz);
    }

    /**
     * Provider for Spring Data MongoDB's {@code @Document} annotation.
     * This provider gracefully handles the case where Spring Data MongoDB
     * is not on the classpath by using reflection.
     */
    class SpringDocumentProvider implements CollectionNameProvider {

        private static final boolean SPRING_AVAILABLE;
        private static final Class<?> DOCUMENT_ANNOTATION_CLASS;
        private static final Method VALUE_METHOD;
        private static final Method COLLECTION_METHOD;

        static {
            boolean springAvailable = false;
            Class<?> documentAnnotationClass = null;
            Method valueMethod = null;
            Method collectionMethod = null;

            try {
                documentAnnotationClass = Class.forName("org.springframework.data.mongodb.core.mapping.Document");
                springAvailable = true;
                valueMethod = ReflectionUtils.tryGetMethod(documentAnnotationClass, "value");
                collectionMethod = ReflectionUtils.tryGetMethod(documentAnnotationClass, "collection");
            } catch (ClassNotFoundException e) {
                // Spring Data MongoDB not available on classpath
            }

            SPRING_AVAILABLE = springAvailable;
            DOCUMENT_ANNOTATION_CLASS = documentAnnotationClass;
            VALUE_METHOD = valueMethod;
            COLLECTION_METHOD = collectionMethod;
        }

        @Override
        public Optional<String> resolveCollectionName(Class<?> clazz) {
            if (!SPRING_AVAILABLE) {
                return Optional.empty();
            }

            try {
                @SuppressWarnings("unchecked")
                var documentAnnotation = clazz.getAnnotation((Class<? extends Annotation>) DOCUMENT_ANNOTATION_CLASS);
                if (documentAnnotation != null) {
                    // Try collection() method first (newer versions)
                    var collection = (String) COLLECTION_METHOD.invoke(documentAnnotation);
                    if (collection != null && !collection.isBlank()) {
                        return Optional.of(collection);
                    }

                    // Fall back to value() method (older versions)
                    var value = (String) VALUE_METHOD.invoke(documentAnnotation);
                    if (value != null && !value.isBlank()) {
                        return Optional.of(value);
                    }
                }
            } catch (Exception e) {
                // Silently ignore reflection errors
            }

            return Optional.empty();
        }
    }

    /**
     * Provider for Quarkus MongoDB with Panache's {@code @MongoEntity} annotation.
     * This provider gracefully handles the case where Quarkus MongoDB
     * is not on the classpath by using reflection.
     */
    class QuarkusMongoEntityProvider implements CollectionNameProvider {

        private static final boolean QUARKUS_AVAILABLE;
        private static final Class<?> MONGO_ENTITY_ANNOTATION_CLASS;
        private static final Method COLLECTION_METHOD;

        static {
            boolean quarkusAvailable = false;
            Class<?> mongoEntityAnnotationClass = null;
            Method collectionMethod = null;

            try {
                mongoEntityAnnotationClass = Class.forName("io.quarkus.mongodb.panache.common.MongoEntity");
                quarkusAvailable = true;
                collectionMethod = ReflectionUtils.tryGetMethod(mongoEntityAnnotationClass, "collection");
            } catch (ClassNotFoundException e) {
                // Quarkus MongoDB not available on classpath
            }

            QUARKUS_AVAILABLE = quarkusAvailable;
            MONGO_ENTITY_ANNOTATION_CLASS = mongoEntityAnnotationClass;
            COLLECTION_METHOD = collectionMethod;
        }

        @Override
        public Optional<String> resolveCollectionName(Class<?> clazz) {
            if (!QUARKUS_AVAILABLE) {
                return Optional.empty();
            }
            return ReflectionUtils.getAnotationStringValue(clazz, MONGO_ENTITY_ANNOTATION_CLASS, COLLECTION_METHOD);
        }
    }

    /**
     * Provider for Morphia's {@code @Entity} annotation.
     * Morphia is another popular MongoDB ODM for Java.
     */
    class MorphiaEntityProvider implements CollectionNameProvider {

        private static final boolean MORPHIA_AVAILABLE;
        private static final Class<?> ENTITY_ANNOTATION_CLASS;
        private static final Method VALUE_METHOD;

        static {
            boolean morphiaAvailable = false;
            Class<?> entityAnnotationClass = null;
            Method valueMethod = null;

            try {
                entityAnnotationClass = Class.forName("dev.morphia.annotations.Entity");
                morphiaAvailable = true;
                valueMethod = ReflectionUtils.tryGetMethod(entityAnnotationClass, "value");
            } catch (ClassNotFoundException e) {
                // Try older Morphia package
                try {
                    entityAnnotationClass = Class.forName("org.mongodb.morphia.annotations.Entity");
                    morphiaAvailable = true;
                    valueMethod = ReflectionUtils.tryGetMethod(entityAnnotationClass, "value");
                } catch (ClassNotFoundException ex) {
                    // Morphia not available on classpath
                }
            }

            MORPHIA_AVAILABLE = morphiaAvailable;
            ENTITY_ANNOTATION_CLASS = entityAnnotationClass;
            VALUE_METHOD = valueMethod;
        }

        @Override
        public Optional<String> resolveCollectionName(Class<?> clazz) {
            if (!MORPHIA_AVAILABLE) {
                return Optional.empty();
            }
            return ReflectionUtils.getAnotationStringValue(clazz, ENTITY_ANNOTATION_CLASS, VALUE_METHOD);
        }
    }
}