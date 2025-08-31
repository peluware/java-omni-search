package com.peluware.omnisearch.mongodb.rsql;

import com.peluware.omnisearch.mongodb.resolvers.PropertyNameResolver;
import com.peluware.omnisearch.mongodb.ReflectionUtils;
import cz.jirutka.rsql.parser.ast.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.Filters.*;

@Slf4j
@RequiredArgsConstructor
public class MongoFilterVisitor<T> implements RSQLVisitor<Bson, Void> {

    private final Class<T> documentClass;
    private final RsqlMongoBuilderOptions builderOptions;

    // Cache para evitar reflexión repetida
    private final Map<String, Map<String, Field>> classFieldsCache = new HashMap<>();

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
            var field = findFieldInClassHierarchy(currentClass, attribute);

            if (field == null) {
                log.warn("Field '{}' not found in class hierarchy of '{}'", attribute, currentClass.getName());
                throw new IllegalArgumentException(String.format("Field '%s' not found in class hierarchy of '%s'", attribute, currentClass.getName()));
            }

            var fieldType = field.getType();
            currentClass = (fieldType.isArray() || Collection.class.isAssignableFrom(fieldType))
                    ? ReflectionUtils.getComponentElementType(field)
                    : fieldType;

            if (currentClass == null) {
                throw new IllegalArgumentException(String.format("Could not resolve type for field: '%s' in class: '%s'", attribute, clazz.getName()));
            }

            paths.add(PropertyNameResolver.resolvePropertyName(field));
            log.trace("Resolved field '{}' to path '{}' with type '{}'", attribute, paths.getLast(), currentClass.getName());
        }

        var resolvedPath = String.join(".", paths);
        log.debug("Resolved full path '{}' to '{}' with final type '{}'",
                originalPath, resolvedPath, currentClass.getName());

        return new FieldPath(resolvedPath, currentClass);
    }

    /**
     * Busca un campo en toda la jerarquía de clases, incluyendo clases padre.
     * Utiliza caché para optimizar búsquedas repetidas.
     */
    private Field findFieldInClassHierarchy(Class<?> clazz, String fieldName) {
        var className = clazz.getName();

        // Verificar cache primero
        var cachedFields = classFieldsCache.computeIfAbsent(className, k -> new HashMap<>());
        if (cachedFields.containsKey(fieldName)) {
            return cachedFields.get(fieldName);
        }

        // Buscar en la jerarquía de clases
        Field field = null;
        Class<?> currentClass = clazz;

        while (currentClass != null && field == null) {
            try {
                field = currentClass.getDeclaredField(fieldName);
                log.trace("Found field '{}' in class '{}'", fieldName, currentClass.getName());
            } catch (NoSuchFieldException e) {
                log.trace("Field '{}' not found in class '{}', checking parent class", fieldName, currentClass.getName());
                currentClass = currentClass.getSuperclass();
            }
        }

        // Cache del resultado (incluso si es null para evitar búsquedas futuras)
        cachedFields.put(fieldName, field);

        return field;
    }

    /**
     * Obtiene todos los campos de una clase incluyendo los heredados.
     * Útil para debugging o validaciones adicionales.
     */
    protected Map<String, Field> getAllFieldsFromHierarchy(Class<?> clazz) {
        var className = clazz.getName();
        var cachedFields = classFieldsCache.get(className);

        if (cachedFields != null && !cachedFields.isEmpty()) {
            return new HashMap<>(cachedFields);
        }

        var allFields = new HashMap<String, Field>();
        Class<?> currentClass = clazz;

        while (currentClass != null) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                // Solo agregar si no existe ya (los campos de clases hijas tienen precedencia)
                allFields.putIfAbsent(field.getName(), field);
            }
            currentClass = currentClass.getSuperclass();
        }

        // Actualizar cache
        classFieldsCache.put(className, new HashMap<>(allFields));

        return allFields;
    }

    /**
     * Limpia el cache de campos. Útil para tests o cuando las clases cambian dinámicamente.
     */
    public void clearFieldsCache() {
        classFieldsCache.clear();
        log.debug("Fields cache cleared");
    }

    protected record FieldPath(String path, Class<?> type) {
    }
}