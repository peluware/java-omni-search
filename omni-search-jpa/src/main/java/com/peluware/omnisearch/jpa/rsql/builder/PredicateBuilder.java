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
package com.peluware.omnisearch.jpa.rsql.builder;

import cz.jirutka.rsql.parser.ast.Node;
import com.peluware.omnisearch.jpa.rsql.EntityManagerAdapter;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;

public interface PredicateBuilder {

    /**
     * Create a Predicate from the RSQL AST node.
     *
     * @param node       RSQL AST node.
     * @param entity     The main entity of the query.
     * @param manager     JPA EntityManager.
     * @param tools      Builder tools facade.
     * @return Predicate a predicate representation of the Node.
     * @throws IllegalArgumentException When illegal arguments are found.
     */
    <T> Predicate createPredicate(Node node, From<?, ?> root, Class<T> entity, EntityManagerAdapter manager, BuilderTools tools) throws IllegalArgumentException;
}
