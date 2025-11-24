package com.peluware.omnisearch.mongodb;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.bson.types.ObjectId;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.time.Year;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class ReflectionUtils {

    private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);

    private ReflectionUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Class<?> resolveComponentFieldType(Field field, Class<?> contextClass) {
        var type = resolveFieldType(field, contextClass);
        if (type.isArray()) {
            return type.getComponentType();
        }
        if (Collection.class.isAssignableFrom(type)) {
            var genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType paramType) {
                var typeArgs = TypeUtils.getTypeArguments(contextClass, field.getDeclaringClass());
                var resolved = TypeUtils.unrollVariables(typeArgs, paramType.getActualTypeArguments()[0]);
                if (resolved instanceof Class<?> classType) {
                    return classType;
                }
            }
        }
        throw new IllegalArgumentException("Field is not a collection or array: " + field.getName() + " of type " + type.getName());
    }


    public static boolean isBasicType(Class<?> type) {
        return String.class.isAssignableFrom(type) ||
                UUID.class.isAssignableFrom(type) ||
                Number.class.isAssignableFrom(type) ||
                Boolean.class.isAssignableFrom(type) ||
                Year.class.isAssignableFrom(type) ||
                ObjectId.class.isAssignableFrom(type) ||
                type.isPrimitive() ||
                type.isEnum();
    }

    public static boolean isBasicField(Field field, Class<?> contextClass) {
        try {
            var resolvedType = resolveFieldType(field, contextClass);
            return isBasicType(resolvedType);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBasicCompositeField(Field field, Class<?> contextClass) {
        try {
            var componentType = resolveComponentFieldType(field, contextClass);
            return isBasicType(componentType);
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Resuelve el tipo de un campo considerando genéricos usando Apache Commons TypeUtils.
     */
    public static Class<?> resolveFieldType(Field field, Class<?> contextClass) {
        try {
            var genericType = field.getGenericType();

            // Si no es un tipo genérico, retornar el raw type
            if (genericType instanceof Class) {
                return (Class<?>) genericType;
            }

            // Intentar resolver usando TypeUtils de Apache Commons
            var typeArguments = TypeUtils.getTypeArguments(contextClass, field.getDeclaringClass());

            if (!typeArguments.isEmpty()) {
                // Sustituir las variables de tipo con sus valores reales
                var resolvedType = TypeUtils.unrollVariables(typeArguments, genericType);

                // Obtener el raw type del tipo resuelto
                var rawType = TypeUtils.getRawType(resolvedType, contextClass);
                if (rawType != null) {
                    log.debug("Resolved generic type for field '{}' from '{}' to '{}'", field.getName(), genericType, rawType.getName());
                    return rawType;
                }
            }

            // Fallback: usar el raw type del campo directamente
            var rawType = TypeUtils.getRawType(genericType, contextClass);
            if (rawType != null) {
                log.debug("Using raw type '{}' for field '{}'", rawType.getName(), field.getName());
                return rawType;
            }

        } catch (Exception e) {
            log.warn("Error resolving type for field '{}': {}", field.getName(), e.getMessage());
        }

        // Último fallback: tipo básico del campo
        return field.getType();
    }

    public static Optional<String> getAnotationStringValue(AnnotatedElement annotatedElement, Class<?> annotationClass, Method valueAccesor) {
        try {
            @SuppressWarnings("unchecked")
            var annotation = annotatedElement.getAnnotation((Class<? extends Annotation>) annotationClass);
            if (annotation != null) {
                var value = (String) valueAccesor.invoke(annotation);

                if (value != null && !value.isBlank()) {
                    return Optional.of(value);
                }
            }
        } catch (Exception e) {
            // Silently ignore reflection errors
        }

        return Optional.empty();
    }

    public static @Nullable Method tryGetMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            // Method not found, return null
            return null;
        }
    }

}
