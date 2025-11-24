package com.peluware.omnisearch.mongodb;

import com.peluware.omnisearch.EnumSearchCandidate;
import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.mongodb.rsql.DefaultRsqlMongoBuilderOptions;
import com.peluware.omnisearch.mongodb.rsql.MongoFilterVisitor;
import com.peluware.omnisearch.mongodb.rsql.RsqlMongoBuilderOptions;
import com.peluware.omnisearch.utils.ParseNumber;
import com.peluware.omnisearch.mongodb.resolvers.PropertyNameResolver;
import cz.jirutka.rsql.parser.RSQLParser;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Year;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;


/**
 * Default implementation of {@link MongoOmniSearchFilterBuilder}.
 * <p>
 * Builds MongoDB {@link Bson} filters based on {@link OmniSearchBaseOptions},
 * combining text-based search across basic and complex fields, propagated
 * properties, and optional RSQL filtering.
 */
public class DefaultMongoOmniSearchFilterBuilder implements MongoOmniSearchFilterBuilder {

    /**
     * Pattern used to detect UUID values in string form.
     */
    protected static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /**
     * Pattern used to detect MongoDB ObjectId values in string form.
     */
    protected static final Pattern OBJECT_ID_PATTERN = Pattern.compile("^[0-9a-fA-F]{24}$");

    private static final Map<Class<?>, List<Field>> BASIC_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> COMPLEX_FIELDS = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(DefaultMongoOmniSearchFilterBuilder.class);

    protected static List<Field> getBasicFields(Class<?> clazz) {
        return BASIC_FIELDS.computeIfAbsent(clazz, c -> Arrays.stream(c.getDeclaredFields())
                .filter(field -> ReflectionUtils.isBasicField(field, clazz) || ReflectionUtils.isBasicCompositeField(field, clazz))
                .toList());
    }

    protected static List<Field> getComplexFields(Class<?> clazz) {
        return COMPLEX_FIELDS.computeIfAbsent(clazz, c -> Arrays.stream(c.getDeclaredFields())
                .filter(field -> !ReflectionUtils.isBasicField(field, clazz) && !ReflectionUtils.isBasicCompositeField(field, clazz))
                .toList());
    }

    private final RSQLParser rsqlParser;
    private final RsqlMongoBuilderOptions rsqlBuilderOptions;

    public DefaultMongoOmniSearchFilterBuilder(RSQLParser rsqlParser, RsqlMongoBuilderOptions rsqlBuilderOptions) {
        this.rsqlParser = rsqlParser;
        this.rsqlBuilderOptions = rsqlBuilderOptions;
    }

    public DefaultMongoOmniSearchFilterBuilder(RSQLParser rsqlParser) {
        this(rsqlParser, new DefaultRsqlMongoBuilderOptions());
    }

    public DefaultMongoOmniSearchFilterBuilder() {
        this(new RSQLParser());
    }

    @Override
    public <D> Bson buildFilter(Class<D> documentClass, OmniSearchBaseOptions options) {

        Bson filters = new Document();

        var search = options.getSearch();
        if (search != null && !search.isBlank()) {
            filters = searchInAllProperties(search, documentClass, options.getPropagations());
        }

        var query = options.getQuery();
        if (query != null) {
            var node = rsqlParser.parse(query);
            var visitor = new MongoFilterVisitor<>(documentClass, rsqlBuilderOptions);
            var rsqlFilter = node.accept(visitor);
            filters = and(filters, rsqlFilter);
        }

        return filters;
    }

    @SuppressWarnings("java:S135")
    private <D> Bson searchInAllProperties(String search, Class<D> documentClass, Set<String> propagations) {
        // Search in direct properties
        var searchFilters = new ArrayList<>(getSearchFilters(search, documentClass, ""));

        // Search in propagated properties (nested documents and arrays)
        for (var path : propagations) {

            var field = getComplexFields(documentClass).stream()
                    .filter(f -> f.getName().equals(path))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Propagation path '" + path + "' not found in class " + documentClass.getName()));

            var fieldType = field.getType();
            var pathName = PropertyNameResolver.resolvePropertyName(field);

            searchFilters.addAll(getSearchFilters(search, fieldType, pathName + "."));
        }

        return or(searchFilters);
    }

    /**
     * Gets search filters for all searchable properties in a class model.
     */
    @SuppressWarnings("java:S3776")
    protected <D> Collection<Bson> getSearchFilters(String search, Class<D> clazz, String prefix) {
        var filters = new ArrayList<Bson>();

        for (var field : getBasicFields(clazz)) {
            try {

                var originalPropertyName = PropertyNameResolver.resolvePropertyName(field);
                var propertyName = prefix + originalPropertyName;
                var basicPredicates = getBasicPredicates(search, field.getType(), propertyName);

                if (basicPredicates != null) {
                    log.trace("Basic predicate created for property '{}' with value '{}'", propertyName, search);
                    filters.add(basicPredicates);
                    continue;
                }

                var fieldType = field.getType();
                if (fieldType.isArray() || Collection.class.isAssignableFrom(fieldType)) {
                    // For arrays or collections, we need to check the element type
                    var elementType = ReflectionUtils.resolveComponentFieldType(field, clazz);
                    var basicFilter = getBasicPredicates(search, elementType, propertyName);
                    if (basicFilter != null) {
                        log.trace("Basic predicate created for array/collection property '{}' with value '{}'", propertyName, search);
                        filters.add(basicFilter);
                    }
                }

            } catch (IllegalArgumentException e) {
                log.trace("Could not parse search value '{}' for property '{}': {}", search, field.getName(), e.getMessage());
            } catch (Exception e) {
                log.trace("Could not create filter for property '{}': {}", field.getName(), e.getMessage());
            }
        }

        return filters;
    }

    @SuppressWarnings("java:S3776")
    protected static @Nullable Bson getBasicPredicates(String search, Class<?> type, String property) {
        if (String.class.isAssignableFrom(type)) {
            var pattern = Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE);
            return regex(property, pattern);
        }

        if (UUID.class.isAssignableFrom(type) && UUID_PATTERN.matcher(search).matches()) {
            return eq(property, UUID.fromString(search));
        }

        if ((Boolean.class.isAssignableFrom(type) || type == boolean.class) && search.matches("(?i)true|false")) {
            return eq(property, Boolean.parseBoolean(search.toLowerCase()));
        }

        if (Year.class.isAssignableFrom(type) && search.matches("\\d{4}")) {
            return eq(property, Year.parse(search));
        }

        if (ObjectId.class.isAssignableFrom(type) && OBJECT_ID_PATTERN.matcher(search).matches()) {
            return eq(property, new ObjectId(search));
        }

        if (type.isEnum()) {
            @SuppressWarnings("unchecked")
            var candidates = EnumSearchCandidate.collectEnumCandidates((Class<? extends Enum<?>>) type, search);
            if (candidates.isEmpty()) {
                return null;
            }
            return in(property, candidates);
        }

        if ((Number.class.isAssignableFrom(type) || type.isPrimitive()) && search.matches("[+-]?\\d*\\.?\\d+")) {
            for (var parser : ParseNumber.PARSERS) {
                if (type.isAssignableFrom(parser.type())) {
                    return eq(property, parser.parse(search));
                }
            }
        }

        return null;
    }
}