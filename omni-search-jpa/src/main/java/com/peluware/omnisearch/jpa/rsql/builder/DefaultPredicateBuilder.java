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
package com.peluware.omnisearch.jpa.rsql.builder;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.Node;
import com.peluware.omnisearch.jpa.rsql.ComparisonOperatorProxy;
import com.peluware.omnisearch.jpa.rsql.EntityManagerAdapter;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Slf4j
@SuppressWarnings("unchecked")
public class DefaultPredicateBuilder implements PredicateBuilder {

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

    /**
     * Private constructor.
     */
    public DefaultPredicateBuilder() {
        super();
    }

    /**
     * Create a Predicate From<?,?> the RSQL AST node.
     *
     * @param node      RSQL AST node.
     * @param root      From<?,?> that predicate expression paths depends on.
     * @param entity    The main entity of the query.
     * @param manager   JPA EntityManager.
     * @param misc      Facade with all necessary tools for predicate creation.
     * @return Predicate a predicate representation of the Node.
     */
    public <T> Predicate createPredicate(Node node, From<?, ?> root, Class<T> entity, EntityManagerAdapter manager, BuilderTools misc) {
        log.debug("Creating Predicate for: {}", node);

        if (node instanceof LogicalNode logicalNode) {
            return createPredicate(logicalNode, root, entity, manager, misc);
        }

        if (node instanceof ComparisonNode comparisonNode) {
            return createPredicate(comparisonNode, root, manager, misc);
        }

        throw new IllegalArgumentException("Unknown expression type: " + node.getClass());
    }

    /**
     * Create a Predicate From<?,?> the RSQL AST logical node.
     *
     * @param logical        RSQL AST logical node.
     * @param root           From<?,?> that predicate expression paths depends on. 
     * @param entity         The main entity of the query.
     * @param entityManager  JPA EntityManager.
     * @param misc         Facade with all necessary tools for predicate creation.
     * @return Predicate a predicate representation of the Node.
     */
    public <T> Predicate createPredicate(LogicalNode logical, From<?, ?> root, Class<T> entity, EntityManagerAdapter entityManager, BuilderTools misc) {
        log.trace("Creating Predicate for logical node: {}", logical);

        var builder = entityManager.getCriteriaBuilder();

        var predicates = new ArrayList<Predicate>();

        log.trace("Creating Predicates From<?,?> all children nodes.");
        for (var node : logical.getChildren()) {
            predicates.add(createPredicate(node, root, entity, entityManager, misc));
        }

        return switch (logical.getOperator()) {
            case AND -> builder.and(predicates.toArray(new Predicate[0]));
            case OR -> builder.or(predicates.toArray(new Predicate[0]));
        };

    }

    /**
     * Create a Predicate From<?,?> the RSQL AST comparison node.
     *
     * @param comparison     RSQL AST comparison node.
     * @param startRoot      From<?,?> that predicate expression paths depends on.
     * @param entityManager  JPA EntityManager.
     * @param misc         Facade with all necessary tools for predicate creation.
     * @return Predicate a predicate representation of the Node.
     */
    public Predicate createPredicate(ComparisonNode comparison, @NotNull From<?, ?> startRoot, EntityManagerAdapter entityManager, BuilderTools misc) {
        log.trace("Creating Predicate for comparison node: {}", comparison);
        var propertyPath = findPropertyPath(comparison.getSelector(), startRoot, entityManager, misc);

        log.trace("Cast all arguments to type {}.", propertyPath.getJavaType().getName());
        var castedArguments = misc.getArgumentParser().parse(comparison.getArguments(), propertyPath.getJavaType());
        return createPredicate(propertyPath, comparison.getOperator(), castedArguments, entityManager);
    }

    /**
     * Find a property path in the graph From<?,?> startRoot
     *
     * @param propertyPath   The property path to find.
     * @param startRoot      From<?,?> that property path depends on.
     * @param entityManager  JPA EntityManager.
     * @param misc           Facade with all necessary tools for predicate creation.
     * @return The Path for the property path
     * @throws IllegalArgumentException if attribute of the given property name does not exist
     */
    public Path<?> findPropertyPath(String propertyPath, Path<?> startRoot, EntityManagerAdapter entityManager, BuilderTools misc) {
        var graph = propertyPath.split("\\.");

        var metaModel = entityManager.getMetamodel();
        var classMetadata = metaModel.managedType(startRoot.getJavaType());

        var root = startRoot;

        for (var property : graph) {
            var mappedProperty = misc.getPropertiesMapper().translate(property, classMetadata.getJavaType());
            if (!mappedProperty.equals(property)) {
                root = findPropertyPath(mappedProperty, root, entityManager, misc);
                continue;
            }
            if (!hasPropertyName(property, classMetadata)) {
                throw new IllegalArgumentException("Unknown property: " + property + " From<?,?> entity " + classMetadata.getJavaType().getName());
            }

            if (isAssociationType(property, classMetadata)) {
                var associationType = findPropertyType(property, classMetadata);
                var previousClass = classMetadata.getJavaType().getName();
                classMetadata = metaModel.managedType(associationType);
                log.trace("Create a join between {} and {}.", previousClass, classMetadata.getJavaType().getName());

                if (root instanceof From<?, ?> from) {
                    root = from.join(property);
                } else {
                    log.warn("Root is not a From<?, ?> type, cannot create join for property {}. Using get() instead.", property);
                    root = root.get(property);
                }
            } else {
                log.trace("Create property path for type {} property {}.", classMetadata.getJavaType().getName(), mappedProperty);
                root = root.get(property);

                if (isEmbeddedType(property, classMetadata)) {
                    Class<?> embeddedType = findPropertyType(property, classMetadata);
                    classMetadata = metaModel.managedType(embeddedType);
                }
            }
        }

        return root;
    }

    private Predicate createPredicate(Expression<?> propertyPath, ComparisonOperator operator, List<?> arguments, EntityManagerAdapter manager) {
        log.trace("Creating predicate: propertyPath {} {}", operator, arguments);

        var comparisonOperatorProxy = ComparisonOperatorProxy.asEnum(operator);
        if (comparisonOperatorProxy == null) {
            throw new IllegalArgumentException("Unknown operator: " + operator);
        }
        return switch (comparisonOperatorProxy) {
            case EQUAL -> equalPredicate(propertyPath, arguments, manager);
            case NOT_EQUAL -> notEqualPredicate(propertyPath, arguments, manager);
            case GREATER_THAN -> greaterThanPredicate(propertyPath, operator, arguments, manager);
            case GREATER_THAN_OR_EQUAL -> greaterThanOrEqualPredicate(propertyPath, operator, manager, arguments);
            case LESS_THAN -> lessThanPredicate(propertyPath, operator, arguments, manager);
            case LESS_THAN_OR_EQUAL -> lessThanOrEqualPredicate(propertyPath, operator, arguments, manager);
            case IN -> createIn(propertyPath, arguments);
            case NOT_IN -> createNotIn(propertyPath, arguments, manager);
        };
    }

    private Predicate lessThanOrEqualPredicate(Expression<?> propertyPath, ComparisonOperator operator, List<?> arguments, EntityManagerAdapter manager) {
        var argument = arguments.getFirst();
        if (argument instanceof Date casted) {
            return createBetweenThan((Expression<? extends Date>) propertyPath, START_DATE, casted, manager);
        }
        if (argument instanceof Number casted) {
            return createLessEqual((Expression<? extends Number>) propertyPath, casted, manager);
        }
        if (argument instanceof Comparable casted) {
            return createLessEqualComparable((Expression<? extends Comparable>) propertyPath, casted, manager);
        }
        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
    }

    private Predicate lessThanPredicate(Expression<?> propertyPath, ComparisonOperator operator, List<?> arguments, EntityManagerAdapter manager) {
        var argument = arguments.getFirst();
        if (argument instanceof Date casted) {
            int days = -1;
            return createBetweenThan((Expression<? extends Date>) propertyPath, START_DATE, modifyDate(casted, days), manager);
        }
        if (argument instanceof Number casted) {
            return createLessThan((Expression<? extends Number>) propertyPath, casted, manager);
        }
        if (argument instanceof Comparable casted) {
            return createLessThanComparable((Expression<? extends Comparable>) propertyPath, casted, manager);
        }
        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
    }

    private Predicate greaterThanOrEqualPredicate(Expression<?> propertyPath, ComparisonOperator operator, EntityManagerAdapter manager, List<?> arguments) {
        var argument = arguments.getFirst();
        if (argument instanceof Date casted) {
            return createBetweenThan((Expression<? extends Date>) propertyPath, casted, END_DATE, manager);
        }
        if (argument instanceof Number casted) {
            return createGreaterEqual((Expression<? extends Number>) propertyPath, casted, manager);
        }
        if (argument instanceof Comparable casted) {
            return createGreaterEqualComparable((Expression<? extends Comparable>) propertyPath, casted, manager);
        }
        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
    }

    private Predicate greaterThanPredicate(Expression<?> propertyPath, ComparisonOperator operator, List<?> arguments, EntityManagerAdapter manager) {
        var argument = arguments.getFirst();
        if (argument instanceof Date casted) {
            int days = 1;
            return createBetweenThan((Expression<? extends Date>) propertyPath, modifyDate(casted, days), END_DATE, manager);
        }
        if (argument instanceof Number casted) {
            return createGreaterThan((Expression<? extends Number>) propertyPath, casted, manager);
        }
        if (argument instanceof Comparable casted) {
            return createGreaterThanComparable((Expression<? extends Comparable>) propertyPath, casted, manager);
        }
        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
    }

    protected Predicate notEqualPredicate(Expression<?> propertyPath, List<?> arguments, EntityManagerAdapter manager) {
        Object argument = arguments.getFirst();
        if (argument instanceof String casted) {
            return createNotLike((Expression<String>) propertyPath, casted, manager);
        }
        if (isNullArgument(argument)) {
            return createIsNotNull(propertyPath, manager);
        }
        return createNotEqual(propertyPath, argument, manager);
    }

    protected Predicate equalPredicate(Expression<?> propertyPath, List<?> arguments, EntityManagerAdapter manager) {
        var argument = arguments.getFirst();
        if (argument instanceof String casted) {
            return createLike((Expression<String>) propertyPath, casted, manager);
        }
        if (isNullArgument(argument)) {
            return createIsNull(propertyPath, manager);
        }
        return createEqual(propertyPath, argument, manager);
    }

    /**
     * Creates the between than.
     *
     * @param propertyPath the property path
     * @param start the start date
     * @param end the argument
     * @param manager the manager
     * @return the predicate
     */
    protected Predicate createBetweenThan(Expression<? extends Date> propertyPath, Date start, Date end, EntityManagerAdapter manager) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        return builder.between(propertyPath, start, end);
    }

    /**
     * Apply a case-insensitive "like" constraint to the property path. Value
     * should contain wildcards "*" (% in SQL) and "_".
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument with/without wildcards
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createLike(Expression<String> propertyPath, String argument, EntityManagerAdapter manager) {
        var like = argument.replace(LIKE_WILDCARD, '%');
        var builder = manager.getCriteriaBuilder();
        return builder.like(builder.lower(propertyPath), like.toLowerCase());
    }

    /**
     * Apply an "is null" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createIsNull(Expression<?> propertyPath, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.isNull(propertyPath);
    }

    /**
     * Apply an "equal" constraint to property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createEqual(Expression<?> propertyPath, Object argument, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.equal(propertyPath, argument);
    }

    /**
     * Apply a "not equal" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createNotEqual(Expression<?> propertyPath, Object argument, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.notEqual(propertyPath, argument);
    }

    /**
     * Apply a negative case-insensitive "like" constraint to the property path.
     * Value should contains wildcards "*" (% in SQL) and "_".
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument with/without wildcards
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createNotLike(Expression<String> propertyPath, String argument, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.not(createLike(propertyPath, argument, manager));
    }

    /**
     * Apply an "is not null" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createIsNotNull(Expression<?> propertyPath, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.isNotNull(propertyPath);
    }

    /**
     * Apply a "greater than" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument number.
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createGreaterThan(Expression<? extends Number> propertyPath, Number argument, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.gt(propertyPath, argument);
    }

    /**
     * Apply a "greater than" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument.
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected <Y extends Comparable<? super Y>> Predicate createGreaterThanComparable(Expression<? extends Y> propertyPath, Y argument, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.greaterThan(propertyPath, argument);
    }

    /**
     * Apply a "greater than or equal" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument number.
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createGreaterEqual(Expression<? extends Number> propertyPath, Number argument, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.ge(propertyPath, argument);
    }

    /**
     * Apply a "greater than or equal" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument.
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected <Y extends Comparable<? super Y>> Predicate createGreaterEqualComparable(Expression<? extends Y> propertyPath, Y argument, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.greaterThanOrEqualTo(propertyPath, argument);
    }

    /**
     * Apply a "less than" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument number.
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createLessThan(Expression<? extends Number> propertyPath, Number argument, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.lt(propertyPath, argument);
    }

    /**
     * Apply a "less than" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument.
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected <Y extends Comparable<? super Y>> Predicate createLessThanComparable(Expression<? extends Y> propertyPath, Y argument, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.lessThan(propertyPath, argument);
    }

    /**
     * Apply a "less than or equal" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument number.
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createLessEqual(Expression<? extends Number> propertyPath, Number argument, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.le(propertyPath, argument);
    }

    /**
     * Apply a "less than or equal" constraint to the property path.
     *
     * @param propertyPath  Property path that we want to compare.
     * @param argument      Argument.
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected <Y extends Comparable<? super Y>> Predicate createLessEqualComparable(Expression<? extends Y> propertyPath, Y argument, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
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
     * @param manager       JPA EntityManager.
     * @return Predicate a predicate representation.
     */
    protected Predicate createNotIn(Expression<?> propertyPath, List<?> arguments, EntityManagerAdapter manager) {
        var builder = manager.getCriteriaBuilder();
        return builder.not(createIn(propertyPath, arguments));
    }

    /**
     * Verify if a property is an Association type.
     *
     * @param property       Property to verify.
     * @param classMetadata  Metamodel of the class we want to check.
     * @return               <tt>true</tt> if the property is an associantion, <tt>false</tt> otherwise.
     */
    protected <T> boolean isAssociationType(String property, ManagedType<T> classMetadata) {
        return classMetadata.getAttribute(property).isAssociation();
    }

    /**
     * Verify if a property is an Embedded type.
     *
     * @param property       Property to verify.
     * @param classMetadata  Metamodel of the class we want to check.
     * @return               <tt>true</tt> if the property is an embedded attribute, <tt>false</tt> otherwise.
     */
    protected <T> boolean isEmbeddedType(String property, ManagedType<T> classMetadata) {
        return classMetadata.getAttribute(property).getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED;
    }

    /**
     * Verifies if a class metamodel has the specified property.
     *
     * @param property       Property name.
     * @param classMetadata  Class metamodel that may hold that property.
     * @return               <tt>true</tt> if the class has that property, <tt>false</tt> otherwise.
     */
    protected <T> boolean hasPropertyName(String property, ManagedType<T> classMetadata) {
        Set<Attribute<? super T, ?>> names = classMetadata.getAttributes();
        for (Attribute<? super T, ?> name : names) {
            if (name.getName().equals(property)) return true;
        }
        return false;
    }

    /**
     * Get the property Type out of the metamodel.
     *
     * @param property       Property name for type extraction.
     * @param classMetadata  Reference class metamodel that holds property type.
     * @return Class java type for the property, 
     * 						 if the property is a pluralAttribute it will take the bindable java type of that collection.
     */
    protected <T> Class<?> findPropertyType(String property, ManagedType<T> classMetadata) {
        if (classMetadata.getAttribute(property).isCollection()) {
            return ((PluralAttribute<?, ?, ?>) classMetadata.getAttribute(property)).getBindableJavaType();
        }
        return classMetadata.getAttribute(property).getJavaType();
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
