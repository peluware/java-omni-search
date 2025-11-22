package com.peluware.omnisearch;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Represents the base search options including keyword-based search,
 * join associations, and query filtering using RSQL syntax trees.
 */
public class OmniSearchBaseOptions {

    private String search = null;
    private Set<String> propagations = Set.of();
    private String query = null;

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
     * @param query the RSQL query string
     * @return the updated options
     */
    public OmniSearchBaseOptions query(String query) {
        this.query = query;
        return this;
    }

    /**
     * Gets the simple search keyword.
     *
     * @return the search term
     */
    public String getSearch() {
        return search;
    }

    /**
     * Gets the join associations to include in the query.
     *
     * @return the set of propagations
     */
    public Set<String> getPropagations() {
        return propagations;
    }

    /**
     * Gets the RSQL query string used to filter results.
     *
     * @return the RSQL query
     */
    public String getQuery() {
        return query;
    }
}
