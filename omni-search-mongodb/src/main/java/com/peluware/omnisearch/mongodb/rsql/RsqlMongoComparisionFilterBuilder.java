package com.peluware.omnisearch.mongodb.rsql;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import org.bson.conversions.Bson;

import java.util.List;

/**
 * Builds a MongoDB {@link Bson} filter for a single RSQL comparison.
 */
public interface RsqlMongoComparisionFilterBuilder {

    /**
     * Creates a {@link Bson} filter for the given path, type, operator, and arguments.
     *
     * @param path      the document field path to filter
     * @param type      the field type
     * @param operator  the RSQL comparison operator
     * @param arguments the values to compare against
     * @return a {@link Bson} filter representing the comparison
     */
    Bson buildComparisionFilter(String path, Class<?> type, ComparisonOperator operator, List<?> arguments);
}
