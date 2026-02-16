package com.peluware.omnisearch.mongodb.resolvers;

import com.peluware.omnisearch.mongodb.ReflectionUtils;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Utility class for resolving MongoDB field names from Java class fields.
 * This resolver supports multiple annotation sources including Spring Data MongoDB,
 * standard MongoDB driver annotations, and Morphia ODM.
 *
 * <p>The default resolution order is:
 * <ol>
 *   <li>MongoDB driver {@code @BsonProperty} annotation</li>
 *   <li>Spring Data MongoDB {@code @Field} annotation (if available)</li>
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

    private static final CopyOnWriteArrayList<PropertyNameProvider> DEFAULT_PROVIDERS = new CopyOnWriteArrayList<>(List.of(
            new BsonPropertyProvider(),
            new SpringFieldProvider(),
            new MorphiaPropertyProvider()
    ));

    private static final Map<Class<?>, Map<String, String>> PROPERTY_NAME_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Resolves the MongoDB field name for the given Java field using the default providers.
     * This method is thread-safe and can be called from multiple threads.
     *
     * @param field the Java field to resolve the name for
     * @return the resolved MongoDB field name, never null
     */
    public static String resolvePropertyName(Field field) {

        var declaringClass = field.getDeclaringClass();

        Map<String, String> classCache;
        synchronized (PROPERTY_NAME_CACHE) {
            classCache = PROPERTY_NAME_CACHE.computeIfAbsent(
                    declaringClass,
                    k -> new ConcurrentHashMap<>()
            );
        }

        return classCache.computeIfAbsent(field.getName(), name -> {
            for (var provider : DEFAULT_PROVIDERS) {
                var result = provider.resolvePropertyName(field);
                if (result.isPresent()) {
                    return result.get();
                }
            }
            return name;
        });
    }


    /**
     * Adds a custom property name provider to the resolver.
     *
     * @param provider the custom provider to add
     */
    public static void addPropertyNameProvider(PropertyNameProvider provider) {
        if (provider != null) {
            DEFAULT_PROVIDERS.addIfAbsent(provider);
            clearCache();
        }
    }

    /**
     * Adds a custom property name provider at a specific index in the resolver.
     *
     * @param index    the index at which to add the provider
     * @param provider the custom provider to add
     */
    public static void addPropertyNameProvider(int index, PropertyNameProvider provider) {
        if (index < 0 || index > DEFAULT_PROVIDERS.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + DEFAULT_PROVIDERS.size());
        }
        if (provider != null) {
            DEFAULT_PROVIDERS.add(index, provider);
            clearCache();
        }
    }

    /**
     * Removes a custom property name provider from the resolver.
     *
     * @param provider the custom provider to remove
     */
    public static void removePropertyNameProvider(PropertyNameProvider provider) {
        if (provider != null) {
            DEFAULT_PROVIDERS.remove(provider);
            clearCache();
        }
    }

    /**
     * Clears the internal cache.
     */
    public static void clearCache() {
        PROPERTY_NAME_CACHE.clear();
    }

    /**
     * Resets providers to defaults.
     */
    public static void resetProviders() {
        DEFAULT_PROVIDERS.clear();
        DEFAULT_PROVIDERS.addAll(List.of(
                new BsonPropertyProvider(),
                new SpringFieldProvider(),
                new MorphiaPropertyProvider()
        ));
        clearCache();
    }

    @FunctionalInterface
    public interface PropertyNameProvider {
        Optional<String> resolvePropertyName(Field field);
    }

    /**
     * Mongo Driver provider.
     */
    static class BsonPropertyProvider implements PropertyNameProvider {

        @Override
        public Optional<String> resolvePropertyName(Field field) {
            var annotation = field.getAnnotation(BsonProperty.class);
            if (annotation != null) {
                var value = annotation.value();
                if (value != null && !value.isBlank()) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * Spring Data Mongo provider (reflection-based).
     */
    static class SpringFieldProvider implements PropertyNameProvider {

        private static final boolean SPRING_AVAILABLE;
        private static final Class<?> FIELD_ANNOTATION_CLASS;
        private static final Method VALUE_METHOD;

        static {
            boolean available = false;
            Class<?> clazz = null;
            Method method = null;

            try {
                clazz = Class.forName("org.springframework.data.mongodb.core.mapping.Field");
                method = ReflectionUtils.tryGetMethod(clazz, "value");
                available = true;
            } catch (ClassNotFoundException ignored) {
            }

            SPRING_AVAILABLE = available;
            FIELD_ANNOTATION_CLASS = clazz;
            VALUE_METHOD = method;
        }

        @Override
        public Optional<String> resolvePropertyName(Field field) {
            if (!SPRING_AVAILABLE) {
                return Optional.empty();
            }
            return ReflectionUtils.getAnnotationStringValue(field, FIELD_ANNOTATION_CLASS, VALUE_METHOD);
        }
    }

    /**
     * Morphia provider (supports old and new packages).
     */
    static class MorphiaPropertyProvider implements PropertyNameProvider {

        private static final boolean MORPHIA_AVAILABLE;
        private static final Class<?> PROPERTY_ANNOTATION_CLASS;
        private static final Method VALUE_METHOD;

        static {
            boolean available = false;
            Class<?> clazz = null;
            Method method = null;

            try {
                clazz = Class.forName("dev.morphia.annotations.Property");
                method = ReflectionUtils.tryGetMethod(clazz, "value");
                available = true;
            } catch (ClassNotFoundException e) {
                try {
                    clazz = Class.forName("org.mongodb.morphia.annotations.Property");
                    method = ReflectionUtils.tryGetMethod(clazz, "value");
                    available = true;
                } catch (ClassNotFoundException ignored) {
                }
            }

            MORPHIA_AVAILABLE = available;
            PROPERTY_ANNOTATION_CLASS = clazz;
            VALUE_METHOD = method;
        }

        @Override
        public Optional<String> resolvePropertyName(Field field) {
            if (!MORPHIA_AVAILABLE) {
                return Optional.empty();
            }
            return ReflectionUtils.getAnnotationStringValue(field, PROPERTY_ANNOTATION_CLASS, VALUE_METHOD);
        }
    }
}
