/*
 * The MIT License
 *
 * Copyright 2013 Jakub Jirutka <jakub@jirutka.cz>.
 * Copyright 2015 Antonio Rabelo.
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING From<?,?>,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.peluware.omnisearch.jpa.rsql;

import com.peluware.omnisearch.jpa.JpaContext;
import com.peluware.omnisearch.rsql.RsqlUnknowComparisionOperatorException;
import cz.jirutka.rsql.parser.ast.*;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@SuppressWarnings({"unchecked", "rawtypes", "java:S3740"})
public class DefaultRsqlJpaComparisionPredicateBuilder implements RsqlJpaComparisionPredicateBuilder {

    public static final Character LIKE_WILDCARD = '*';

    protected static final Date START_DATE;
    protected static final Date END_DATE;

    static {
        //
        //  Use a date range that Oracle can cope with - apparently the years around 1 BC and 1 AD are messed up in Oracle - known bug
        //
        var cal = Calendar.getInstance();
        cal.set(9999, Calendar.DECEMBER, 31);
        END_DATE = cal.getTime();
        cal.set(5, Calendar.JANUARY, 1);           //      Use Jan 1, 5 AD, since that's where the Roman's sort of got it together with leap years.
        START_DATE = cal.getTime();
    }

    @Override
    public Predicate buildComparisionPredicate(Expression<?> propertyPath, ComparisonOperator operator, List<?> arguments, JpaContext jpaContext) throws RsqlUnknowComparisionOperatorException {
        log.trace("Creating predicate: propertyPath {} {}", operator, arguments);

        if (RSQLOperators.EQUAL.equals(operator)) {
            return equalPredicate(propertyPath, arguments, jpaContext);
        }
        if (RSQLOperators.NOT_EQUAL.equals(operator)) {
            return notEqualPredicate(propertyPath, arguments, jpaContext);
        }
        if (RSQLOperators.GREATER_THAN.equals(operator)) {
            return greaterThanPredicate(propertyPath, operator, arguments, jpaContext);
        }
        if (RSQLOperators.GREATER_THAN_OR_EQUAL.equals(operator)) {
            return greaterThanOrEqualPredicate(propertyPath, operator, jpaContext, arguments);
        }
        if (RSQLOperators.LESS_THAN.equals(operator)) {
            return lessThanPredicate(propertyPath, operator, arguments, jpaContext);
        }
        if (RSQLOperators.LESS_THAN_OR_EQUAL.equals(operator)) {
            return lessThanOrEqualPredicate(propertyPath, operator, arguments, jpaContext);
        }
        if (RSQLOperators.IN.equals(operator)) {
            return createIn(propertyPath, arguments);
        }
        if (RSQLOperators.NOT_IN.equals(operator)) {
            return createNotIn(propertyPath, arguments, jpaContext);
        }

        throw new RsqlUnknowComparisionOperatorException(operator);
    }

    private Predicate lessThanOrEqualPredicate(Expression<?> propertyPath, ComparisonOperator operator, List<?> arguments, JpaContext jpaContext) {
        var argument = arguments.getFirst();
        if (argument instanceof Date casted) {
            return createBetweenThan((Expression<? extends Date>) propertyPath, START_DATE, casted, jpaContext);
        }
        if (argument instanceof Number casted) {
            return createLessEqual((Expression<? extends Number>) propertyPath, casted, jpaContext);
        }
        if (argument instanceof Comparable casted) {
            return createLessEqualComparable((Expression<? extends Comparable>) propertyPath, casted, jpaContext);
        }
        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
    }

    private Predicate lessThanPredicate(Expression<?> propertyPath, ComparisonOperator operator, List<?> arguments, JpaContext jpaContext) {
        var argument = arguments.getFirst();
        if (argument instanceof Date casted) {
            int days = -1;
            return createBetweenThan((Expression<? extends Date>) propertyPath, START_DATE, modifyDate(casted, days), jpaContext);
        }
        if (argument instanceof Number casted) {
            return createLessThan((Expression<? extends Number>) propertyPath, casted, jpaContext);
        }
        if (argument instanceof Comparable casted) {
            return createLessThanComparable((Expression<? extends Comparable>) propertyPath, casted, jpaContext);
        }
        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
    }

    private Predicate greaterThanOrEqualPredicate(Expression<?> propertyPath, ComparisonOperator operator, JpaContext jpaContext, List<?> arguments) {
        var argument = arguments.getFirst();
        if (argument instanceof Date casted) {
            return createBetweenThan((Expression<? extends Date>) propertyPath, casted, END_DATE, jpaContext);
        }
        if (argument instanceof Number casted) {
            return createGreaterEqual((Expression<? extends Number>) propertyPath, casted, jpaContext);
        }
        if (argument instanceof Comparable casted) {
            return createGreaterEqualComparable((Expression<? extends Comparable>) propertyPath, casted, jpaContext);
        }
        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
    }

    private Predicate greaterThanPredicate(Expression<?> propertyPath, ComparisonOperator operator, List<?> arguments, JpaContext jpaContext) {
        var argument = arguments.getFirst();
        if (argument instanceof Date casted) {
            int days = 1;
            return createBetweenThan((Expression<? extends Date>) propertyPath, modifyDate(casted, days), END_DATE, jpaContext);
        }
        if (argument instanceof Number casted) {
            return createGreaterThan((Expression<? extends Number>) propertyPath, casted, jpaContext);
        }
        if (argument instanceof Comparable casted) {
            return createGreaterThanComparable((Expression<? extends Comparable>) propertyPath, casted, jpaContext);
        }
        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
    }

    protected Predicate notEqualPredicate(Expression<?> propertyPath, List<?> arguments, JpaContext jpaContext) {
        var argument = arguments.getFirst();
        if (argument instanceof String casted) {
            return createNotLike((Expression<String>) propertyPath, casted, jpaContext);
        }
        if (isNullArgument(argument)) {
            return createIsNotNull(propertyPath, jpaContext);
        }
        return createNotEqual(propertyPath, argument, jpaContext);
    }

    protected Predicate equalPredicate(Expression<?> propertyPath, List<?> arguments, JpaContext jpaContext) {
        var argument = arguments.getFirst();
        if (argument instanceof String casted) {
            return createLike((Expression<String>) propertyPath, casted, jpaContext);
        }
        if (isNullArgument(argument)) {
            return createIsNull(propertyPath, jpaContext);
        }
        return createEqual(propertyPath, argument, jpaContext);
    }

    /**
     * Creates the between than.
     *
     * @param propertyPath the property path
     * @param start the start date
     * @param end the argument
     * @param jpaContext the manager
     * @return the predicate
     */
    protected Predicate createBetweenThan(Expression<? extends Date> propertyPath, Date start, Date end, JpaContext jpaContext) {
        var builder = jpaContext.getCriteriaBuilder();
        return builder.between(propertyPath, start, end);
    }

    /**
     * Apply a case-insensitive "like" constraint to the property path. Value
     * should contain wildcards "*" (% in SQL) and "_".
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument with/without wildcards
     * @param jpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createLike(Expression<String> propertyPath, String argument, JpaContext jpaContext) {
        var like = argument.replace(LIKE_WILDCARD, '%');
        var builder = jpaContext.getCriteriaBuilder();
        return builder.like(builder.lower(propertyPath), like.toLowerCase());
    }

    /**
     * Apply an "is null" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param JpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createIsNull(Expression<?> propertyPath, JpaContext JpaContext) {
        var builder = JpaContext.getCriteriaBuilder();
        return builder.isNull(propertyPath);
    }

    /**
     * Apply an "equal" constraint to property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument
     * @param JpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createEqual(Expression<?> propertyPath, Object argument, JpaContext JpaContext) {
        var builder = JpaContext.getCriteriaBuilder();
        return builder.equal(propertyPath, argument);
    }

    /**
     * Apply a "not equal" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument
     * @param jpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createNotEqual(Expression<?> propertyPath, Object argument, JpaContext jpaContext) {
        var builder = jpaContext.getCriteriaBuilder();
        return builder.notEqual(propertyPath, argument);
    }

    /**
     * Apply a negative case-insensitive "like" constraint to the property path.
     * Value should contains wildcards "*" (% in SQL) and "_".
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument with/without wildcards
     * @param jpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createNotLike(Expression<String> propertyPath, String argument, JpaContext jpaContext) {
        var builder = jpaContext.getCriteriaBuilder();
        return builder.not(createLike(propertyPath, argument, jpaContext));
    }

    /**
     * Apply an "is not null" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param jpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createIsNotNull(Expression<?> propertyPath, JpaContext jpaContext) {
        var builder = jpaContext.getCriteriaBuilder();
        return builder.isNotNull(propertyPath);
    }

    /**
     * Apply a "greater than" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument number.
     * @param jpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createGreaterThan(Expression<? extends Number> propertyPath, Number argument, JpaContext jpaContext) {
        var builder = jpaContext.getCriteriaBuilder();
        return builder.gt(propertyPath, argument);
    }

    /**
     * Apply a "greater than" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument.
     * @param jpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected <Y extends Comparable<? super Y>> Predicate createGreaterThanComparable(Expression<? extends Y> propertyPath, Y argument, JpaContext jpaContext) {
        var builder = jpaContext.getCriteriaBuilder();
        return builder.greaterThan(propertyPath, argument);
    }

    /**
     * Apply a "greater than or equal" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument number.
     * @param JpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createGreaterEqual(Expression<? extends Number> propertyPath, Number argument, JpaContext JpaContext) {
        var builder = JpaContext.getCriteriaBuilder();
        return builder.ge(propertyPath, argument);
    }

    /**
     * Apply a "greater than or equal" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument.
     * @param jpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected <Y extends Comparable<? super Y>> Predicate createGreaterEqualComparable(Expression<? extends Y> propertyPath, Y argument, JpaContext jpaContext) {
        var builder = jpaContext.getCriteriaBuilder();
        return builder.greaterThanOrEqualTo(propertyPath, argument);
    }

    /**
     * Apply a "less than" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument number.
     * @param jpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createLessThan(Expression<? extends Number> propertyPath, Number argument, JpaContext jpaContext) {
        var builder = jpaContext.getCriteriaBuilder();
        return builder.lt(propertyPath, argument);
    }

    /**
     * Apply a "less than" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument.
     * @param jpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected <Y extends Comparable<? super Y>> Predicate createLessThanComparable(Expression<? extends Y> propertyPath, Y argument, JpaContext jpaContext) {
        var builder = jpaContext.getCriteriaBuilder();
        return builder.lessThan(propertyPath, argument);
    }

    /**
     * Apply a "less than or equal" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument number.
     * @param jpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createLessEqual(Expression<? extends Number> propertyPath, Number argument, JpaContext jpaContext) {
        var builder = jpaContext.getCriteriaBuilder();
        return builder.le(propertyPath, argument);
    }

    /**
     * Apply a "less than or equal" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument.
     * @param jpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected <Y extends Comparable<? super Y>> Predicate createLessEqualComparable(Expression<? extends Y> propertyPath, Y argument, JpaContext jpaContext) {
        var builder = jpaContext.getCriteriaBuilder();
        return builder.lessThanOrEqualTo(propertyPath, argument);
    }

    /**
     * Apply a "in" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param arguments     List of arguments.
     * @return Predicate a predicate representation.
     */
    protected Predicate createIn(Expression<?> propertyPath, List<?> arguments) {
        return propertyPath.in(arguments);
    }

    /**
     * Apply a "not in" (out) constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param arguments     List of arguments.
     * @param jpaContext       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createNotIn(Expression<?> propertyPath, List<?> arguments, JpaContext jpaContext) {
        var builder = jpaContext.getCriteriaBuilder();
        return builder.not(createIn(propertyPath, arguments));
    }


    protected boolean isNullArgument(Object argument) {
        return argument == null;
    }


    /**
     * Get date regarding the operation (less then or greater than)
     *
     * @param date Date to be modified
     * @param days Days to be added or removed form argument;
     *@return Date modified date
     */
    protected Date modifyDate(Date date, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, days);
        date = c.getTime();
        return date;
    }

    /**
     * Builds an error message that reports that the argument is not suitable for use with the comparison operator.
     * @param operator operator From<?,?> the RSQL query
     * @param argument actual argument produced From<?,?> the ArgumentParser
     * @return Error message for use in an Exception
     */
    protected String buildNotComparableMessage(ComparisonOperator operator, Object argument) {
        return String.format("Invalid type for comparison operator: %s type: %s must implement Comparable<%s>", operator, argument.getClass().getName(), argument.getClass().getSimpleName());
    }
}
