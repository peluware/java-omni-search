package com.peluware.omnisearch.mongodb;

import lombok.experimental.UtilityClass;
import org.bson.types.ObjectId;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.Year;
import java.util.Collection;
import java.util.UUID;

@UtilityClass
public class ReflectUtils {

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
}
