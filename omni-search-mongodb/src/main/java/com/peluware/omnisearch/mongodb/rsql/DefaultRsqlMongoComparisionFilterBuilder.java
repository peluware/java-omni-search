package com.peluware.omnisearch.mongodb.rsql;

import com.peluware.omnisearch.rsql.RsqlUnknowComparisionOperatorException;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;

import java.util.List;

import static com.mongodb.client.model.Filters.*;

@Slf4j
public class DefaultRsqlMongoComparisionFilterBuilder implements RsqlMongoComparisionFilterBuilder {


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

        throw new RsqlUnknowComparisionOperatorException(operator);
    }
}
