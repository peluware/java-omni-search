package com.peluware.omnisearch.jpa;

import lombok.experimental.UtilityClass;

@UtilityClass
class PrimitiveUtils {
    /**
     * Gets the wrapper type for a primitive class.
     *
     * @param primitiveType the primitive type class
     * @return the corresponding wrapper class
     */
    static Class<?> getWrapperType(Class<?> primitiveType) {
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == long.class) return Long.class;
        if (primitiveType == double.class) return Double.class;
        if (primitiveType == float.class) return Float.class;
        if (primitiveType == boolean.class) return Boolean.class;
        if (primitiveType == char.class) return Character.class;
        if (primitiveType == byte.class) return Byte.class;
        if (primitiveType == short.class) return Short.class;
        return primitiveType;
    }
}
