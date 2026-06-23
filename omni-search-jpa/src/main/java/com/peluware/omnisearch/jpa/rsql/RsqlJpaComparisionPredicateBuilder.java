package com.peluware.omnisearch.jpa.rsql;

import com.peluware.omnisearch.rsql.RsqlUnknowComparisionOperatorException;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.List;

/**
 * Builds a JPA {@link Predicate} for a single RSQL comparison.
 */
public interface RsqlJpaComparisionPredicateBuilder {

    /**
     * Creates a {@link Predicate} for the given property path, operator, and arguments.
     *
     * @param propertyPath the expression representing the entity property
     * @param operator     the RSQL comparison operator
     * @param arguments    the values to compare against
     * @param cb           the criteria builder
     * @return a {@link Predicate} representing the comparison
     * @throws RsqlUnknowComparisionOperatorException if the operator is not supported
     */
    Predicate buildComparisionPredicate(
            Expression<?> propertyPath,
            ComparisonOperator operator,
            List<?> arguments,
            CriteriaBuilder cb
    ) throws RsqlUnknowComparisionOperatorException;
}
