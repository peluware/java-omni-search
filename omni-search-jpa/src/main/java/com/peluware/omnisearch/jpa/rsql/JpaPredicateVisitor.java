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

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import com.peluware.omnisearch.jpa.rsql.builder.AbstractJpaVisitor;
import com.peluware.omnisearch.jpa.rsql.builder.BuilderTools;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JpaPredicateVisitor<T> extends AbstractJpaVisitor<Predicate, T> implements RSQLVisitor<Predicate, EntityManagerAdapter> {

    private final Root<T> root;

    public JpaPredicateVisitor(Root<T> t, BuilderTools builderTools) {
        super(t.getJavaType(), builderTools);
        this.root = t;
    }


    @Override
    public Predicate visit(AndNode node, EntityManagerAdapter entityManager) {
        log.debug("Creating Predicate for AndNode: {}", node);
        return getBuilderTools().getPredicateBuilder().createPredicate(node, root, entityClass, entityManager, getBuilderTools());
    }

    @Override
    public Predicate visit(OrNode node, EntityManagerAdapter entityManager) {
        log.debug("Creating Predicate for OrNode: {}", node);
        return getBuilderTools().getPredicateBuilder().createPredicate(node, root, entityClass, entityManager, getBuilderTools());
    }

    @Override
    public Predicate visit(ComparisonNode node, EntityManagerAdapter entityManager) {
        log.debug("Creating Predicate for ComparisonNode: {}", node);
        return getBuilderTools().getPredicateBuilder().createPredicate(node, root, entityClass, entityManager, getBuilderTools());
    }
}
