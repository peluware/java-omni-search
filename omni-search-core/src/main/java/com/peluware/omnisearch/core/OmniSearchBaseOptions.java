package com.peluware.omnisearch.core;

import cz.jirutka.rsql.parser.ast.Node;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Represents the base search options including keyword-based search,
 * join associations, and query filtering using RSQL syntax trees.
 */
@Getter
public class OmniSearchBaseOptions {

    private String search = null;
    private Set<String> joins = Set.of();
    private Node query = null;

    /**
     * Sets a simple search keyword to be used in the query.
     *
     * @param search the search term
     * @return the updated options
     */
    public OmniSearchBaseOptions search(String search) {
        this.search = search;
        return this;
    }

    /**
     * Specifies the join associations to include in the query.
     *
     * @param joins the set of entity associations to join
     * @return the updated options
     */
    public OmniSearchBaseOptions joins(@NotNull Set<String> joins) {
        this.joins = joins;
        return this;
    }

    /**
     * Specifies the join associations using varargs.
     *
     * @param joins the entity associations to join
     * @return the updated options
     */
    public OmniSearchBaseOptions joins(String @NotNull ... joins) {
        return joins(Set.of(joins));
    }

    /**
     * Sets the RSQL AST node used to filter results.
     *
     * @param query the RSQL query node
     * @return the updated options
     */
    public OmniSearchBaseOptions query(Node query) {
        this.query = query;
        return this;
    }
}
