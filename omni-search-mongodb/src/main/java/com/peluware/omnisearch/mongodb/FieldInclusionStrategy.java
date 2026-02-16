package com.peluware.omnisearch.mongodb;

import java.lang.reflect.Field;

public interface FieldInclusionStrategy {
    boolean include(Field field);
}
