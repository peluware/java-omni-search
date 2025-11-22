package com.peluware.omnisearch.mongodb;

import com.peluware.domain.Order;
import com.peluware.omnisearch.OmniSearch;
import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.OmniSearchOptions;
import com.mongodb.client.MongoDatabase;
import com.peluware.omnisearch.mongodb.resolvers.CollectionNameResolver;
import com.peluware.omnisearch.mongodb.rsql.DefaultRsqlMongoBuilderOptions;
import com.peluware.omnisearch.mongodb.rsql.RsqlMongoBuilderOptions;
import cz.jirutka.rsql.parser.RSQLParser;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MongoDB-based implementation of the {@link OmniSearch} interface,
 * allowing flexible querying, sorting, and pagination of MongoDB documents.
 * Uses the native MongoDB Java Driver without any framework dependencies.
 */
public class MongoOmniSearch implements OmniSearch {

    private static final Logger log = LoggerFactory.getLogger(MongoOmniSearch.class);

    private final MongoDatabase database;
    private final MongoOmniSearchFilterBuilder filterBuilder;

    public MongoOmniSearch(MongoDatabase database, MongoOmniSearchFilterBuilder filterBuilder) {
        this.database = database;
        this.filterBuilder = filterBuilder;
    }

    public MongoOmniSearch(MongoDatabase database, RSQLParser rsqlParser, RsqlMongoBuilderOptions rsqlBuilderOptions) {
        this(database, new DefaultMongoOmniSearchFilterBuilder(rsqlParser, rsqlBuilderOptions));
    }

    public MongoOmniSearch(MongoDatabase database, RSQLParser rsqlParser) {
        this(database, rsqlParser, new DefaultRsqlMongoBuilderOptions());
    }

    public MongoOmniSearch(MongoDatabase database) {
        this(database, new RSQLParser());
    }

    @Override
    public <E> List<E> list(Class<E> entityClass, OmniSearchOptions options) {

        var collectionName = CollectionNameResolver.resolveCollectionName(entityClass);
        var collection = database.getCollection(collectionName, entityClass);

        var filter = filterBuilder.buildFilter(entityClass, options);

        var findIterable = collection.find(filter);

        // JSON con formato pretty
        debugJsonFilter(filter);

        // Apply sorting
        var sort = options.getSort();
        if (sort.isSorted()) {
            var sortDocument = new Document();
            for (var order : sort.orders()) {
                sortDocument.append(order.property(), order.direction() == Order.Direction.ASC ? 1 : -1);
            }
            findIterable = findIterable.sort(sortDocument);
        }

        // Apply pagination
        var pagination = options.getPagination();
        if (pagination.isPaginated()) {
            findIterable = findIterable
                    .skip(pagination.getNumber() * pagination.getSize())
                    .limit(pagination.getSize());
        }

        return Collections.unmodifiableList(findIterable.into(new ArrayList<>()));
    }


    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation constructs a count query based on the filtering criteria
     * provided in the {@link OmniSearchBaseOptions}.
     * </p>
     */
    @Override
    public <E> long count(Class<E> entityClass, OmniSearchBaseOptions options) {

        var collectionName = CollectionNameResolver.resolveCollectionName(entityClass);
        var collection = database.getCollection(collectionName, entityClass);

        var filter = filterBuilder.buildFilter(entityClass, options);

        debugJsonFilter(filter);

        log.debug("Executing MongoDB count query: {} for entity: {}", filter, entityClass.getSimpleName());
        return collection.countDocuments(filter);
    }


    private void debugJsonFilter(Bson filter) {
        if (log.isDebugEnabled()) {
            var settings = JsonWriterSettings.builder()
                    .indent(true)
                    .build();

            log.debug("Filter (JSON): {}", filter
                    .toBsonDocument(BsonDocument.class, database.getCodecRegistry())
                    .toJson(settings));
        }
    }

}