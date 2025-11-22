package com.peluware.omnisearch.mongodb.rsql;

import com.peluware.omnisearch.rsql.RsqlUnknowComparisionOperatorException;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.mongodb.client.model.Filters.*;

/**
 * Default implementation of {@link RsqlMongoComparisionFilterBuilder} that
 * builds MongoDB {@link Bson} filters for standard RSQL comparison operators.
 */
public class DefaultRsqlMongoComparisionFilterBuilder implements RsqlMongoComparisionFilterBuilder {

    private static final Logger log = LoggerFactory.getLogger(DefaultRsqlMongoComparisionFilterBuilder.class);

    /**
     * Builds a MongoDB {@link Bson} filter for the given field path, type, operator, and arguments.
     * <p>
     * Supports standard RSQL operators: {@code ==, !=, >, >=, <, <=, in, out}.
     *
     * @param path      the document field path
     * @param type      the field type (not used in this default implementation)
     * @param operator  the RSQL comparison operator
     * @param arguments the values to compare against
     * @return a {@link Bson} filter representing the comparison
     * @throws RsqlUnknowComparisionOperatorException if the operator is not supported
     */
    @Override
    public Bson buildComparisionFilter(String path, Class<?> type, ComparisonOperator operator, List<?> arguments) {
        log.debug("Building comparison filter for path: {}, type: {}, operator: {}, arguments: {}", path, type, operator, arguments);

        if (RSQLOperators.EQUAL.equals(operator)) {
            return eq(path, arguments.getFirst());
        }
        if (RSQLOperators.NOT_EQUAL.equals(operator)) {
            return ne(path, arguments.getFirst());
        }
        if (RSQLOperators.GREATER_THAN.equals(operator)) {
            return gt(path, arguments.getFirst());
        }
        if (RSQLOperators.GREATER_THAN_OR_EQUAL.equals(operator)) {
            return gte(path, arguments.getFirst());
        }
        if (RSQLOperators.LESS_THAN.equals(operator)) {
            return lt(path, arguments.getFirst());
        }
        if (RSQLOperators.LESS_THAN_OR_EQUAL.equals(operator)) {
            return lte(path, arguments.getFirst());
        }
        if (RSQLOperators.IN.equals(operator)) {
            return in(path, arguments);
        }
        if (RSQLOperators.NOT_IN.equals(operator)) {
            return nin(path, arguments);
        }

        throw new RsqlUnknowComparisionOperatorException(operator.getSymbol());
    }
}
