package com.peluware.omnisearch.mongodb.resolvers;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.lang.reflect.Field;

@FunctionalInterface
public interface PropertyNameResolver {

    PropertyNameResolver DEFAULT = field -> {
        if (field.isAnnotationPresent(BsonProperty.class)) {
            var annotation = field.getAnnotation(BsonProperty.class);
            if (annotation.value() != null && !annotation.value().isBlank()) {
                return annotation.value();
            }
        }
        return field.getName();
    };

    String resolvePropertyName(Field field);
}
