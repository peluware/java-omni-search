package com.peluware.omnisearch.mongodb;

import com.peluware.omnisearch.core.OmniSearch;
import com.peluware.omnisearch.core.OmniSearchBaseOptions;
import com.peluware.omnisearch.core.OmniSearchOptions;
import com.mongodb.client.MongoDatabase;
import com.peluware.omnisearch.mongodb.resolvers.CollectionNameResolver;
import com.peluware.omnisearch.mongodb.rsql.MongoFilterVisitor;
import com.peluware.omnisearch.mongodb.rsql.RsqlMongoBuilderOptions;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.and;

/**
 * MongoDB-based implementation of the {@link OmniSearch} interface,
 * allowing flexible querying, sorting, and pagination of MongoDB documents.
 * Uses the native MongoDB Java Driver without any framework dependencies.
 */
@Slf4j
@RequiredArgsConstructor
public class MongoOmniSearch implements OmniSearch {


    private final MongoDatabase database;
    private final RsqlMongoBuilderOptions rsqlBuilderOptions;

    public MongoOmniSearch(MongoDatabase database) {
        this(database, RsqlMongoBuilderOptions.DEFAULT);
    }

    @Setter
    private MongoOmniSearchFilterBuilder filterBuilder = MongoOmniSearchFilterBuilder.DEFAULT;


    @Override
    public <E> List<E> search(Class<E> entityClass, OmniSearchOptions options) {

        var collectionName = CollectionNameResolver.resolveCollectionName(entityClass);
        var collection = database.getCollection(collectionName, entityClass);

        var filter = buildFilter(entityClass, options);

        var findIterable = collection.find(filter);

        // JSON con formato pretty
        debugJsonFilter(filter);

        // Apply sorting
        var sort = options.getSort();
        if (sort.isSorted()) {
            var sortDocument = new Document();
            for (var order : sort.orders()) {
                sortDocument.append(order.property(), order.ascending() ? 1 : -1);
            }
            findIterable = findIterable.sort(sortDocument);
        }

        // Apply pagination
        var pagination = options.getPagination();
        if (pagination.isPaginated()) {
            findIterable = findIterable
                    .skip(pagination.offset())
                    .limit(pagination.size());
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

        var filter = buildFilter(entityClass, options);

        debugJsonFilter(filter);

        log.debug("Executing MongoDB count query: {} for entity: {}", filter, entityClass.getSimpleName());
        return collection.countDocuments(filter);
    }

    private <E> Bson buildFilter(Class<E> entityClass, OmniSearchBaseOptions options) {
        var filter = filterBuilder.buildFilter(entityClass, options);
        var query = options.getQuery();
        if (query != null) {
            var rsqlFilter = query.accept(new MongoFilterVisitor<>(entityClass, rsqlBuilderOptions), null);
            filter = and(filter, rsqlFilter);
        }
        return filter;
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