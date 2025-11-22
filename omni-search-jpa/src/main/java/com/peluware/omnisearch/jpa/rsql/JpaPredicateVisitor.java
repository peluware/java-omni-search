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

import com.peluware.omnisearch.jpa.JpaContext;
import cz.jirutka.rsql.parser.ast.*;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;

public class JpaPredicateVisitor<T> extends AbstractJpaVisitor<Predicate, T> {

    private static final Logger log = LoggerFactory.getLogger(JpaPredicateVisitor.class);

    private final Root<T> root;

    public JpaPredicateVisitor(Root<T> root, RsqlJpaBuilderOptions builderTools) {
        super(root.getJavaType(), builderTools);
        this.root = root;
    }

    @Override
    public Predicate visit(AndNode node, JpaContext jpaContext) {
        log.debug("Creating Predicate for AndNode: {}", node);
        return visitLogicalNode(node, jpaContext);
    }

    @Override
    public Predicate visit(OrNode node, JpaContext jpaContext) {
        log.debug("Creating Predicate for OrNode: {}", node);
        return visitLogicalNode(node, jpaContext);
    }


    @Override
    public Predicate visit(ComparisonNode node, JpaContext jpaContext) {
        log.debug("Creating Predicate for ComparisonNode: {}", node);

        var argumentParser = builderOptions.getArgumentParser();

        var path = findPath(node.getSelector(), root, jpaContext);
        var type = path.getModel().getBindableJavaType();

        log.trace("Cast all arguments to type {}.", type.getName());
        var castedArguments = argumentParser.parse(node.getArguments(), type);

        var comparisionPredicateBuilder = builderOptions.getComparisionPredicateBuilder();

        return comparisionPredicateBuilder.buildComparisionPredicate(
                path,
                node.getOperator(),
                castedArguments,
                jpaContext
        );
    }

    private Predicate visitLogicalNode(LogicalNode node, JpaContext jpaContext) {
        var cb = jpaContext.getCriteriaBuilder();
        var children = node.getChildren();
        if (children.isEmpty()) {
            return cb.disjunction();
        }

        var predicates = new ArrayList<Predicate>();
        for (var childNode : children) {
            predicates.add(childNode.accept(this, jpaContext));
        }

        return switch (node.getOperator()) {
            case OR -> cb.or(predicates.toArray(new Predicate[0]));
            case AND -> cb.and(predicates.toArray(new Predicate[0]));
        };
    }


    /**
     * Find a property path in the graph From startRoot
     *
     * @param path   The property path to find.
     * @param startRoot      From that property path depends on.
     * @param entityManager  JPA EntityManager.
     * @return The Path for the property path
     * @throws IllegalArgumentException if attribute of the given property name does not exist
     */
    public Path<?> findPath(String path, Path<?> startRoot, JpaContext entityManager) {
        var graph = path.split("\\.");

        var metaModel = entityManager.getMetamodel();
        var classMetadata = metaModel.managedType(startRoot.getJavaType());
        var currentRoot = startRoot;

        var graphLength = graph.length;
        for (int i = 0; i < graphLength; i++) {
            var attribute = graph[i];

            if (!hasAttribute(attribute, classMetadata)) {
                throw new IllegalArgumentException("Unknown property: " + attribute + " From<?,?> entity " + classMetadata.getJavaType().getName());
            }

            var jpaAttribute = classMetadata.getAttribute(attribute);
            var persistentAttributeType = jpaAttribute.getPersistentAttributeType();
            var type = getSingularType(jpaAttribute);

            if (jpaAttribute.isAssociation()) {

                classMetadata = metaModel.managedType(type);
                currentRoot = ((From<?, ?>) currentRoot).join(attribute);

            } else if (persistentAttributeType == PersistentAttributeType.EMBEDDED) {

                classMetadata = metaModel.embeddable(type);
                currentRoot = currentRoot.get(attribute);

            } else if (persistentAttributeType == PersistentAttributeType.ELEMENT_COLLECTION) {

                currentRoot = ((From<?, ?>) currentRoot).join(attribute);
                if (i != graphLength - 1) {
                    throw new IllegalArgumentException("ElementCollection must be the last part of the path: " + path);
                }

            } else if (persistentAttributeType == PersistentAttributeType.BASIC) {

                currentRoot = currentRoot.get(attribute);
                if (i != graphLength - 1) {
                    throw new IllegalArgumentException("Basic attribute must be the last part of the path: " + path);
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
    protected <E> boolean hasAttribute(String property, ManagedType<E> classMetadata) {
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
     * @return Class java type for the property,
     * 						 if the property is a pluralAttribute it will take the bindable java type of that collection.
     */
    protected Class<?> getSingularType(Attribute<?, ?> property) {
        if (property.isCollection()) {
            return ((PluralAttribute<?, ?, ?>) property).getBindableJavaType();
        }
        return property.getJavaType();
    }
}
