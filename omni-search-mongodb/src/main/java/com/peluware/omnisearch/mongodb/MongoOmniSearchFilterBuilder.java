package com.peluware.omnisearch.mongodb;

import com.peluware.omnisearch.OmniSearchBaseOptions;
import org.bson.conversions.Bson;

/**
 * Builds a MongoDB {@link Bson} filter based on the given document class
 * and {@link OmniSearchBaseOptions}.
 */
public interface MongoOmniSearchFilterBuilder {

    /**
     * Creates a {@link Bson} filter for the specified document class using
     * the provided search options.
     *
     * @param documentClass the document class to search
     * @param options       the base search options
     * @param <D>           the type of the document
     * @return a {@link Bson} filter for MongoDB queries
     */
    <D> Bson buildFilter(Class<D> documentClass, OmniSearchBaseOptions options);
}
