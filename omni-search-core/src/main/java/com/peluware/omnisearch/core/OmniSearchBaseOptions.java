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
    private Set<String> propagations = Set.of();
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
     * @param propagations entities or sub-objects to propagate search in the query
     * @return the updated options
     */
    public OmniSearchBaseOptions propagations(@NotNull Set<String> propagations) {
        this.propagations = propagations;
        return this;
    }

    /**
     * Specifies the join associations using varargs.
     *
     * @param propagations entities o sub-objects to propagate search in the query
     * @return the updated options
     */
    public OmniSearchBaseOptions propagations(String @NotNull ... propagations) {
        return propagations(Set.of(propagations));
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
