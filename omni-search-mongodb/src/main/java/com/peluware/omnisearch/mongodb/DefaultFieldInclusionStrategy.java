package com.peluware.omnisearch.mongodb;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

public final class DefaultFieldInclusionStrategy implements FieldInclusionStrategy {

    private static final Set<String> KNOWN_TRANSIENTS = Set.of(
            "org.springframework.data.annotation.Transient",
            "dev.morphia.annotations.Transient",
            "org.mongodb.morphia.annotations.Transient"
    );

    @Override
    public boolean include(Field field) {

        if (Modifier.isTransient(field.getModifiers())) {
            return false;
        }

        if (Modifier.isStatic(field.getModifiers())) {
            return false;
        }

        for (var annotation : field.getAnnotations()) {
            if (KNOWN_TRANSIENTS.contains(annotation.annotationType().getName())) {
                return false;
            }
        }

        return true;
    }
}
