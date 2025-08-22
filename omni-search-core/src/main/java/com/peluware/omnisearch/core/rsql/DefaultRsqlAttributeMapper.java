/*
 * The MIT License
 *
 * Copyright 2013 Jakub Jirutka <jakub@jirutka.cz>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.peluware.omnisearch.core.rsql;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Simple implementation of the {@link RsqlAttributeMapper}.
 *
 * @author Jakub Jirutka <jakub@jirutka.cz>
 */
@Setter
@Getter
@Slf4j
public class DefaultRsqlAttributeMapper implements RsqlAttributeMapper {


    private Map<Class<?>, Map<String, String>> mapping;


    /**
     * Construct new <tt>SimpleMapper</tt> with zero initial capacity of the
     * entities map.
     */
    public DefaultRsqlAttributeMapper() {
        this(0);
    }

    /**
     * Construct new <tt>SimpleMapper</tt> with the specified initial capacity
     * of the entities map.
     *
     * @param initialCapacity initial capacity of entities map
     */
    public DefaultRsqlAttributeMapper(int initialCapacity) {
        mapping = HashMap.newHashMap(initialCapacity);
    }

    public String map(String selector, Class<?> entityClass) {
        if (mapping.isEmpty()) return selector;

        var map = mapping.get(entityClass);
        var property = (map != null) ? map.get(selector) : null;

        if (property != null) {
            log.debug("Found mapping {} -> {}", selector, property);
            return property;
        }

        return selector;
    }


    /**
     * Add selectors -> property names mapping for given entity class.
     *
     * @param entityClass entity class
     * @param mapping mapping of selectors to property names
     */
    public void addMapping(Class<?> entityClass, Map<String, String> mapping) {
        this.mapping.put(entityClass, mapping);
    }

    /**
     * Add one selector -> property name mapping for given entity class.
     *
     * @param entityClass entity class
     * @param selector Selector that identifies some element of an entry's content.
     * @param property Name of corresponding entity's property.
     */
    public void addMapping(Class<?> entityClass, String selector, String property) {
        mapping.get(entityClass).put(selector, property);
    }

}