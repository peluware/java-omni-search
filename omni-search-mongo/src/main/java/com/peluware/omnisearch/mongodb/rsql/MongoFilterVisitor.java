package com.peluware.omnisearch.mongodb.rsql;

import com.peluware.omnisearch.mongodb.resolvers.PropertyNameResolver;
import com.peluware.omnisearch.mongodb.ReflectUtils;
import cz.jirutka.rsql.parser.ast.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;

import static com.mongodb.client.model.Filters.*;

@Slf4j
@RequiredArgsConstructor
public class MongoFilterVisitor<T> implements RSQLVisitor<Bson, Void> {

    private final Class<T> documentClass;
    private final RsqlMongoBuilderOptions builderOptions;
    private final PropertyNameResolver propertyNameResolver;

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
        var graph = originalPath.split("\\.");
        var currentClass = clazz;

        var paths = new ArrayList<String>();

        for (var attribute : graph) {
            try {
                var field = currentClass.getDeclaredField(attribute);
                var fieldType = field.getType();

                currentClass = (fieldType.isArray() || Collection.class.isAssignableFrom(fieldType))
                        ? ReflectUtils.getComponentElementType(field)
                        : fieldType;

                if (currentClass == null) {
                    throw new IllegalArgumentException("Could not resolve type for field: " + attribute + " in class: " + clazz.getName());
                }

                paths.add(propertyNameResolver.resolvePropertyName(field));

            } catch (NoSuchFieldException e) {
                log.trace("Field '{}' not found in class '{}'.", attribute, currentClass.getName());
            }
        }

        return new FieldPath(
                String.join(".", paths),
                currentClass
        );
    }

    protected record FieldPath(String path, Class<?> type) {
    }

}
