/*
 * The MIT License
 *
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.peluware.omnisearch.jpa.rsql;

import cz.jirutka.rsql.parser.ast.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Set;

@Slf4j
public class JpaPredicateVisitor<T> extends AbstractJpaVisitor<Predicate, T> implements RSQLVisitor<Predicate, EntityManager> {

    private final Root<T> root;

    public JpaPredicateVisitor(Root<T> root, RsqlJpaBuilderTools builderTools) {
        super(root.getJavaType(), builderTools);
        this.root = root;
    }

    @Override
    public Predicate visit(AndNode node, EntityManager entityManager) {
        log.debug("Creating Predicate for AndNode: {}", node);
        return visitLogicalNode(node, entityManager);
    }

    @Override
    public Predicate visit(OrNode node, EntityManager entityManager) {
        log.debug("Creating Predicate for OrNode: {}", node);
        return visitLogicalNode(node, entityManager);
    }

    private Predicate visitLogicalNode(LogicalNode node, EntityManager entityManager) {
        var cb = entityManager.getCriteriaBuilder();
        var children = node.getChildren();
        if (children.isEmpty()) {
            return cb.disjunction();
        }

        var predicates = new ArrayList<Predicate>();
        for (var childNode : children) {
            predicates.add(childNode.accept(this, entityManager));
        }

        return switch (node.getOperator()) {
            case OR -> cb.or(predicates.toArray(new Predicate[0]));
            case AND -> cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public Predicate visit(ComparisonNode node, EntityManager entityManager) {
        log.debug("Creating Predicate for ComparisonNode: {}", node);

        var comparisionPredicateBuilder = builderTools.getComparisionPredicateBuilder();
        var argumentParser = builderTools.getArgumentParser();

        var propertyPath = findPropertyPath(node.getSelector(), root, entityManager);

        log.trace("Cast all arguments to type {}.", propertyPath.getJavaType().getName());
        var castedArguments = argumentParser.parse(node.getArguments(), propertyPath.getJavaType());

        return comparisionPredicateBuilder.buildComparisionPredicate(
                propertyPath,
                node.getOperator(),
                castedArguments,
                entityManager
        );
    }


    /**
     * Find a property path in the graph From<?,?> startRoot
     *
     * @param propertyPath   The property path to find.
     * @param startRoot      From<?,?> that property path depends on.
     * @param entityManager  JPA EntityManager.
     * @return The Path for the property path
     * @throws IllegalArgumentException if attribute of the given property name does not exist
     */
    public Path<?> findPropertyPath(String propertyPath, Path<?> startRoot, EntityManager entityManager) {
        var graph = propertyPath.split("\\.");

        var metaModel = entityManager.getMetamodel();
        var classMetadata = metaModel.managedType(startRoot.getJavaType());

        var currentRoot = startRoot;
        var attributeMapper = builderTools.getAttributeMapper();

        for (var attribute : graph) {
            var mappedProperty = attributeMapper.map(attribute, classMetadata.getJavaType());
            if (!mappedProperty.equals(attribute)) {
                currentRoot = findPropertyPath(mappedProperty, currentRoot, entityManager);
                continue;
            }

            if (!hasPropertyName(attribute, classMetadata)) {
                throw new IllegalArgumentException("Unknown property: " + attribute + " From<?,?> entity " + classMetadata.getJavaType().getName());
            }

            if (isAssociationType(attribute, classMetadata)) {
                var associationType = findPropertyType(attribute, classMetadata);
                var previousClass = classMetadata.getJavaType().getName();
                classMetadata = metaModel.managedType(associationType);
                log.trace("Create a join between {} and {}.", previousClass, classMetadata.getJavaType().getName());

                if (currentRoot instanceof From<?, ?> from) {
                    currentRoot = from.join(attribute);
                } else {
                    log.warn("Root is not a From<?, ?> type, cannot create join for property {}. Using get() instead.", attribute);
                    currentRoot = currentRoot.get(attribute);
                }
            } else {
                log.trace("Create property path for type {} property {}.", classMetadata.getJavaType().getName(), mappedProperty);
                currentRoot = currentRoot.get(attribute);

                if (isEmbeddedType(attribute, classMetadata)) {
                    var embeddedType = findPropertyType(attribute, classMetadata);
                    classMetadata = metaModel.managedType(embeddedType);
                }
            }
        }

        return currentRoot;
    }

    /**
     * Verifies if a class metamodel has the specified property.
     *
     * @param property       Property name.
     * @param classMetadata  Class metamodel that may hold that property.
     * @return               <tt>true</tt> if the class has that property, <tt>false</tt> otherwise.
     */
    protected <E> boolean hasPropertyName(String property, ManagedType<E> classMetadata) {
        Set<Attribute<? super E, ?>> names = classMetadata.getAttributes();
        for (Attribute<? super E, ?> name : names) {
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
    protected <E> Class<?> findPropertyType(String property, ManagedType<E> classMetadata) {
        if (classMetadata.getAttribute(property).isCollection()) {
            return ((PluralAttribute<?, ?, ?>) classMetadata.getAttribute(property)).getBindableJavaType();
        }
        return classMetadata.getAttribute(property).getJavaType();
    }


    /**
     * Verify if a property is an Association type.
     *
     * @param property       Property to verify.
     * @param classMetadata  Metamodel of the class we want to check.
     * @return               <tt>true</tt> if the property is an associantion, <tt>false</tt> otherwise.
     */
    protected <E> boolean isAssociationType(String property, ManagedType<E> classMetadata) {
        return classMetadata.getAttribute(property).isAssociation();
    }

    /**
     * Verify if a property is an Embedded type.
     *
     * @param property       Property to verify.
     * @param classMetadata  Metamodel of the class we want to check.
     * @return               <tt>true</tt> if the property is an embedded attribute, <tt>false</tt> otherwise.
     */
    protected <E> boolean isEmbeddedType(String property, ManagedType<E> classMetadata) {
        return classMetadata.getAttribute(property).getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED;
    }


}
