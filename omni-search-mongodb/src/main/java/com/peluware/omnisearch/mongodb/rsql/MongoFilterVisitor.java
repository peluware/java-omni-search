package com.peluware.omnisearch.mongodb.rsql;

import com.peluware.omnisearch.mongodb.resolvers.PropertyNameResolver;
import com.peluware.omnisearch.mongodb.ReflectionUtils;
import cz.jirutka.rsql.parser.ast.*;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.mongodb.client.model.Filters.*;

public class MongoFilterVisitor<T> implements RSQLVisitor<Bson, Void> {

    private static final Logger log = LoggerFactory.getLogger(MongoFilterVisitor.class);
    private static final Map<String, FieldPath> FIELD_PATH_CACHE = new ConcurrentHashMap<>();

    private final Class<T> documentClass;
    private final RsqlMongoBuilderOptions builderOptions;

    // Cache simple para evitar reflexión repetida

    public MongoFilterVisitor(Class<T> documentClass, RsqlMongoBuilderOptions builderOptions) {
        this.documentClass = documentClass;
        this.builderOptions = builderOptions;
    }

    public MongoFilterVisitor(Class<T> documentClass) {
        this(documentClass, new DefaultRsqlMongoBuilderOptions());
    }

    @Override
    public Bson visit(AndNode node, Void param) {
        log.debug("Creating Bson for AndNode: {}", node);
        return visitLogicalNode(node);
    }

    @Override
    public Bson visit(OrNode node, Void param) {
        log.debug("Creating Bson for OrNode: {}", node);
        return visitLogicalNode(node);
    }

    private Bson visitLogicalNode(LogicalNode node) {
        var children = node.getChildren();
        if (children.isEmpty()) {
            return where("false"); // disjunction equivalent
        }

        var filters = new ArrayList<Bson>();
        for (var childNode : children) {
            filters.add(childNode.accept(this, null));
        }

        return switch (node.getOperator()) {
            case OR -> or(filters);
            case AND -> and(filters);
        };
    }

    @Override
    public Bson visit(ComparisonNode node, Void param) {
        log.debug("Creating Predicate for ComparisonNode: {}", node);
        var argumentParser = builderOptions.getArgumentParser();

        var fieldPath = findFieldType(node.getSelector(), documentClass);
        var castedArguments = argumentParser.parse(node.getArguments(), fieldPath.type);

        var comparisionFilterBuilder = builderOptions.getComparisionFilterBuilder();

        return comparisionFilterBuilder.buildComparisionFilter(
                fieldPath.path,
                fieldPath.type,
                node.getOperator(),
                castedArguments
        );
    }

    protected FieldPath findFieldType(String originalPath, Class<?> clazz) {
        // Usar cache
        var cacheKey = clazz.getName() + "#" + originalPath;
        var cached = FIELD_PATH_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        var graph = originalPath.split("\\.");
        var currentClass = clazz;
        var paths = new ArrayList<String>();

        for (var attribute : graph) {
            // Usar Apache Commons FieldUtils - maneja herencia automáticamente
            var field = FieldUtils.getField(currentClass, attribute, true);

            if (field == null) {
                log.trace("Field '{}' not found in class hierarchy of '{}'", attribute, currentClass.getName());
                throw new IllegalArgumentException(String.format("Field '%s' not found in class hierarchy of '%s'", attribute, currentClass.getName()));
            }

            // Resolver tipo usando Apache Commons TypeUtils para manejo de genéricos
            var resolvedClass = ReflectionUtils.resolveFieldType(field, currentClass);

            currentClass = (resolvedClass.isArray() || Collection.class.isAssignableFrom(resolvedClass))
                    ? ReflectionUtils.resolveComponentFieldType(field, currentClass)
                    : resolvedClass;

            paths.add(PropertyNameResolver.resolvePropertyName(field));
            log.trace("Resolved field '{}' to path '{}' with type '{}'", attribute, paths.getLast(), currentClass.getName());
        }

        var resolvedPath = String.join(".", paths);
        var result = new FieldPath(resolvedPath, currentClass);

        // Cache del resultado
        FIELD_PATH_CACHE.put(cacheKey, result);

        log.debug("Resolved full path '{}' to '{}' with final type '{}'", originalPath, resolvedPath, currentClass.getName());

        return result;
    }


    /**
     * Limpia el cache. Útil para tests.
     */
    public static void clearCache() {
        FIELD_PATH_CACHE.clear();
        log.debug("Field path cache cleared");
    }


    protected record FieldPath(String path, Class<?> type) {
    }
}