package com.peluware.omnisearch.mongodb.resolvers;

import com.peluware.omnisearch.mongodb.ReflectionUtils;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for resolving MongoDB field names from Java class fields.
 * This resolver supports multiple annotation sources including Spring Data MongoDB,
 * standard MongoDB driver annotations, and Morphia ODM.
 *
 * <p>The default resolution order is:
 * <ol>
 *   <li>Spring Data MongoDB {@code @Field} annotation (if available)</li>
 *   <li>MongoDB driver {@code @BsonProperty} annotation</li>
 *   <li>Morphia {@code @Property} annotation (if available)</li>
 *   <li>Raw field name as fallback</li>
 * </ol>
 *
 * @author Peluware
 * @since 1.0
 */
@SuppressWarnings("java:S1192")
public final class PropertyNameResolver {

    private PropertyNameResolver() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final List<PropertyNameProvider> DEFAULT_PROVIDERS = new ArrayList<>(List.of(
            new BsonPropertyProvider(),
            new SpringFieldProvider(),
            new MorphiaPropertyProvider()
    ));

    private static final Map<Field, String> PROPERTY_NAME_CACHE = new ConcurrentHashMap<>();

    /**
     * Resolves the MongoDB field name for the given Java field using the default providers.
     * This method is thread-safe and can be called from multiple threads.
     *
     * @param field the Java field to resolve the name for
     * @return the resolved MongoDB field name, never null
     */
    public static String resolvePropertyName(Field field) {
        return PROPERTY_NAME_CACHE.computeIfAbsent(field, f -> {
            for (var provider : DEFAULT_PROVIDERS) {
                var result = provider.resolvePropertyName(f);
                if (result.isPresent()) {
                    return result.get();
                }
            }
            return f.getName(); // fallback
        });
    }

    /**
     * Adds a custom property name provider to the resolver.
     * @param provider the custom provider to add
     */
    public static void addPropertyNameProvider(PropertyNameProvider provider) {
        if (provider != null) {
            DEFAULT_PROVIDERS.add(provider);
        }
    }

    /**
     * Adds a custom property name provider at a specific index in the resolver.
     * @param index the index at which to add the provider
     * @param provider the custom provider to add
     */
    public static void addPropertyNameProvider(int index, PropertyNameProvider provider) {
        if (provider != null) {
            DEFAULT_PROVIDERS.add(index, provider);
        }
    }


    /**
     * A provider interface for resolving property names from specific annotation sources.
     * Implementations should return {@link Optional#empty()} if they cannot resolve
     * the field name from their specific annotation source.
     */
    @FunctionalInterface
    public interface PropertyNameProvider {
        /**
         * Attempts to resolve the property name for the given field.
         *
         * @param field the field to resolve the name for
         * @return an Optional containing the resolved name, or empty if this provider
         *         cannot resolve the name from its annotation source
         */
        Optional<String> resolvePropertyName(Field field);
    }

    /**
     * Provider for MongoDB driver's {@code @BsonProperty} annotation.
     * This is the standard annotation for the official MongoDB Java driver.
     */
    static class BsonPropertyProvider implements PropertyNameProvider {

        @Override
        public Optional<String> resolvePropertyName(Field field) {
            if (field.isAnnotationPresent(BsonProperty.class)) {
                var annotation = field.getAnnotation(BsonProperty.class);
                if (annotation.value() != null && !annotation.value().isBlank()) {
                    return Optional.of(annotation.value());
                }
            }
            return Optional.empty();
        }
    }

    /**
     * Provider for Spring Data MongoDB's {@code @Field} annotation.
     * This provider gracefully handles the case where Spring Data MongoDB
     * is not on the classpath by using reflection.
     */
    static class SpringFieldProvider implements PropertyNameProvider {

        private static final boolean SPRING_AVAILABLE;
        private static final Class<?> FIELD_ANNOTATION_CLASS;
        private static final Method VALUE_METHOD;

        static {
            boolean springAvailable = false;
            Class<?> fieldAnnotationClass = null;
            Method method = null;

            try {
                fieldAnnotationClass = Class.forName("org.springframework.data.mongodb.core.mapping.Field");
                springAvailable = true;
                method = ReflectionUtils.tryGetMethod(fieldAnnotationClass, "value");
            } catch (ClassNotFoundException e) {
                // Spring Data MongoDB not available on classpath
            }

            SPRING_AVAILABLE = springAvailable;
            FIELD_ANNOTATION_CLASS = fieldAnnotationClass;
            VALUE_METHOD = method;
        }

        @Override
        public Optional<String> resolvePropertyName(Field field) {
            if (!SPRING_AVAILABLE) {
                return Optional.empty();
            }
            return ReflectionUtils.getAnotationStringValue(field, FIELD_ANNOTATION_CLASS, VALUE_METHOD);
        }
    }


    /**
     * Provider for Morphia's {@code @Property} annotation.
     * Morphia is another popular MongoDB ODM for Java.
     */
    static class MorphiaPropertyProvider implements PropertyNameProvider {

        private static final boolean MORPHIA_AVAILABLE;
        private static final Class<?> PROPERTY_ANNOTATION_CLASS;
        private static final Method VALUE_METHOD;

        static {
            boolean morphiaAvailable = false;
            Class<?> propertyAnnotationClass = null;
            Method method = null;

            try {
                propertyAnnotationClass = Class.forName("dev.morphia.annotations.Property");
                morphiaAvailable = true;
                method = ReflectionUtils.tryGetMethod(propertyAnnotationClass, "value");
            } catch (ClassNotFoundException e) {
                // Try older Morphia package
                try {
                    propertyAnnotationClass = Class.forName("org.mongodb.morphia.annotations.Property");
                    morphiaAvailable = true;
                    method = ReflectionUtils.tryGetMethod(propertyAnnotationClass, "value");
                } catch (ClassNotFoundException ex) {
                    // Morphia not available on classpath
                }
            }

            MORPHIA_AVAILABLE = morphiaAvailable;
            PROPERTY_ANNOTATION_CLASS = propertyAnnotationClass;
            VALUE_METHOD = method;
        }

        @Override
        public Optional<String> resolvePropertyName(Field field) {
            if (!MORPHIA_AVAILABLE) {
                return Optional.empty();
            }
            return ReflectionUtils.getAnotationStringValue(field, PROPERTY_ANNOTATION_CLASS, VALUE_METHOD);
        }
    }

}