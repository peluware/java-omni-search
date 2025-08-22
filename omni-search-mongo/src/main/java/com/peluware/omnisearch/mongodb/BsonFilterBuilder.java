package com.peluware.omnisearch.mongodb;

import com.peluware.omnisearch.core.OmniSearchBaseOptions;
import com.peluware.omnisearch.core.rsql.RsqlBuilderTools;
import org.bson.conversions.Bson;

public interface BsonFilterBuilder {

    BsonFilterBuilder DEFAULT = new DefaultBsonFilterBuilder();


    /**
     * Resolves the ClassModel for the given entity class.
     *
     * @param documentClass the entity class to resolve
     * @param <D>         the type of the entity
     * @return the ClassModel for the entity class
     */
    <D> Bson resolveFilter(Class<D> documentClass, OmniSearchBaseOptions options, RsqlBuilderTools rsqlBuilderTools);


}