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
import com.peluware.omnisearch.jpa.JpaUtils;
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

        var path = JpaUtils.findPath(node.getSelector(), root, jpaContext);
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



}
