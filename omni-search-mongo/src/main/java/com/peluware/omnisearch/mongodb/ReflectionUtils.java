package com.peluware.omnisearch.mongodb;

import lombok.experimental.UtilityClass;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.time.Year;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@UtilityClass
public class ReflectionUtils {

    public static Class<?> getComponentElementType(Field field) {
        var type = field.getType();

        if (type.isArray()) {
            return type.getComponentType();
        }
        if (Collection.class.isAssignableFrom(type)) {
            var genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType paramType) {
                var typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> classType) {
                    return classType;
                }
            }
        }
        return null;
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

    public static boolean isBasicField(Field field) {
        return isBasicType(field.getType());
    }

    public static boolean isBasicCompositeField(Field field) {
        var componentType = getComponentElementType(field);
        if (componentType == null) {
            return false;
        }
        return isBasicType(componentType);
    }

    public static Optional<String> getAnotationStringValue(AnnotatedElement annotatedElement, Class<?> annotationClass, Method valueAccesor) {
        try {
            @SuppressWarnings("unchecked")
            var annotation = annotatedElement.getAnnotation((Class<? extends Annotation>) annotationClass);
            if (annotation != null) {
                String value = (String) valueAccesor.invoke(annotation);

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
