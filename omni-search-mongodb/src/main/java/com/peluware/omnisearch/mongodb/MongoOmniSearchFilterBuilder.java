package com.peluware.omnisearch.mongodb;

import com.peluware.omnisearch.core.OmniSearchBaseOptions;
import org.bson.conversions.Bson;

public interface MongoOmniSearchFilterBuilder {

    MongoOmniSearchFilterBuilder DEFAULT = new DefaultMongoOmniSearchFilterBuilder();


    /**
     * Resolves the ClassModel for the given entity class.
     *
     * @param documentClass the entity class to resolve
     * @param options       the search options
     * @param <D>         the type of the entity
     * @return the ClassModel for the entity class
     */
    <D> Bson buildFilter(Class<D> documentClass, OmniSearchBaseOptions options);
}