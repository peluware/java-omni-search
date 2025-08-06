package com.peluware.omnisearch.jpa;


import com.peluware.omnisearch.core.OmniSearchBaseOptions;
import com.peluware.omnisearch.core.OmniSearchOptions;
import com.peluware.omnisearch.jpa.rsql.builder.BuilderTools;
import com.peluware.omnisearch.jpa.rsql.JpaPredicateVisitor;
import com.peluware.omnisearch.jpa.rsql.EntityManagerAdapter;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.time.Year;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility class responsible for building JPA Criteria {@link Predicate} objects
 * based on {@link OmniSearchBaseOptions}, supporting search across all columns,
 * embedded fields, element collections, and associations.
 *
 * <p>It supports filtering using RSQL nodes and handles special types like UUID, numbers,
 * booleans, enums, and {@link java.time.Year}.</p>
 */
@Slf4j
@UtilityClass
public class JpaOmniSearchPredicateBuilder {

    /**
     * Pattern used to detect UUID values in string form.
     */
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /**
     * Builds a {@link Predicate} that searches across all basic, embeddable, and simple element collection fields.
     *
     * @param search      the search keyword
     * @param root        the root entity
     * @param em          the {@link EntityManager}
     * @param joinColumns the associations to join for searching
     * @param <E>         the entity type
     * @return a combined OR predicate for all matched fields
     */
    private static <E> Predicate searchInAllColumns(
            @NotNull String search,
            Root<E> root,
            EntityManager em,
            Set<String> joinColumns
    ) {
        var predicates = new ArrayList<>(getSearchPredicates(search, root, em));

        var managedType = em.getMetamodel().managedType(root.getJavaType());
        for (var joinColumn : joinColumns) {
            if (!managedType.getAttribute(joinColumn).isAssociation()) {
                log.trace("Join column '{}' is not an association in {}", joinColumn, managedType.getJavaType().getName());
                continue;
            }

            var join = root.join(joinColumn, JoinType.LEFT);
            predicates.addAll(getSearchPredicates(search, join, em));
        }

        var cb = em.getCriteriaBuilder();
        return cb.or(predicates.toArray(Predicate[]::new));
    }

    /**
     * Retrieves a list of predicates for all eligible attributes under a given {@link Path}.
     *
     * @param search the search keyword
     * @param path   the current path to inspect
     * @param em     the {@link EntityManager}
     * @return a collection of predicates matching the search keyword
     */
    private static Collection<Predicate> getSearchPredicates(String search, Path<?> path, EntityManager em) {
        var javaType = path.getJavaType();
        var managedType = em.getMetamodel().managedType(javaType);
        var predicates = new ArrayList<Predicate>();

        for (var attribute : managedType.getAttributes()) {
            var propertyName = attribute.getName();
            var type = attribute.getPersistentAttributeType();

            if (type == Attribute.PersistentAttributeType.BASIC) {
                predicates.addAll(getBasicPredicates(search, path.get(propertyName), em));
            } else if (type == Attribute.PersistentAttributeType.EMBEDDED) {
                predicates.addAll(getSearchPredicates(search, path.get(propertyName), em));
            } else if (type == Attribute.PersistentAttributeType.ELEMENT_COLLECTION &&
                    attribute instanceof PluralAttribute<?, ?, ?> plural &&
                    path instanceof From<?, ?> from &&
                    plural.getElementType().getPersistenceType() == Type.PersistenceType.BASIC) {
                predicates.addAll(getBasicPredicates(search, from.join(propertyName, JoinType.LEFT), em));
            }
        }

        return predicates;
    }

    /**
     * Retrieves a list of matching enum constants based on the search term.
     *
     * @param enumType the enum type
     * @param value    the value to match
     * @return a set of matched enum constants
     */
    private static Collection<Enum<?>> searchEnumCandidates(Class<? extends Enum<?>> enumType, String value) {
        var set = new HashSet<Enum<?>>();
        for (var constant : enumType.getEnumConstants()) {
            if (constant.name().toLowerCase().contains(value.toLowerCase()) ||
                    (constant instanceof JpaEnumSearchCandidate c && c.isCandidate(value))) {
                set.add(constant);
            }
        }
        return set;
    }

    /**
     * Builds predicates for simple types like String, UUID, Number, Boolean, Year, and Enums.
     *
     * @param search the search term
     * @param path   the path to the attribute
     * @param em     the entity manager
     * @return a collection of predicates
     */
    private static Collection<Predicate> getBasicPredicates(String search, Path<?> path, EntityManager em) {
        var type = path.getJavaType();
        var cb = em.getCriteriaBuilder();

        if (String.class.isAssignableFrom(type)) {
            return Collections.singleton(cb.like(cb.lower(path.as(String.class)), "%" + search.toLowerCase() + "%"));
        }

        if (UUID.class.isAssignableFrom(type) && UUID_PATTERN.matcher(search).matches()) {
            return Collections.singleton(cb.equal(path, UUID.fromString(search)));
        }

        if ((Number.class.isAssignableFrom(type) || type.isPrimitive()) && search.matches("\\d+")) {
            var predicates = new ArrayList<Predicate>();
            for (var parser : ParseNumber.PARSERS) {
                if (type.isAssignableFrom(parser.type())) {
                    predicates.add(cb.equal(path, parser.parse(search)));
                }
            }
            return predicates;
        }

        if ((Boolean.class.isAssignableFrom(type) || type == boolean.class) && search.matches("true|false")) {
            return Collections.singleton(cb.equal(path, Boolean.parseBoolean(search)));
        }

        if (Year.class.isAssignableFrom(type) && search.matches("\\d{4}")) {
            return Collections.singleton(cb.equal(path, Year.parse(search)));
        }

        if (type.isEnum()) {
            @SuppressWarnings("unchecked")
            var candidates = searchEnumCandidates((Class<? extends Enum<?>>) type, search);
            return candidates.isEmpty() ? List.of() : Collections.singleton(path.in(candidates));
        }

        return List.of();
    }

    /**
     * Builds the {@link SearchQuery} used for the search operation.
     *
     * @param em           the entity manager
     * @param entityClass  the entity class to query
     * @param options      the full search options
     * @param builderTools tools to build the query structure
     * @param <E>          the entity type
     * @return a search query including criteria builder, query, and root
     */
    <E> SearchQuery<E, E> buildSearchWhereSpec(
            EntityManager em,
            Class<E> entityClass,
            OmniSearchOptions options,
            BuilderTools builderTools
    ) {
        return buildSearchWhereSpec(em, entityClass, entityClass, options, builderTools);
    }

    /**
     * Builds the {@link SearchQuery} for custom result types (e.g., projections).
     *
     * @param em           the entity manager
     * @param queryClass   the result class of the query
     * @param entityClass  the entity class to search
     * @param options      the base search options
     * @param builderTools tools to build the query structure
     * @param <Q>          the query result type
     * @param <E>          the entity type
     * @return a search query containing builder, query, and root
     */
    <Q, E> SearchQuery<Q, E> buildSearchWhereSpec(
            EntityManager em,
            Class<Q> queryClass,
            Class<E> entityClass,
            OmniSearchBaseOptions options,
            BuilderTools builderTools
    ) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(queryClass);
        var root = query.from(entityClass);
        var predicate = buildPredicate(em, root, options, builderTools);
        query.where(predicate);
        return new SearchQuery<>(cb, query, root);
    }

    /**
     * Builds a {@link Predicate} based on provided options using default builder tools.
     *
     * @param em      the entity manager
     * @param root    the root path of the query
     * @param options the search options
     * @param <M>     the entity type
     * @return the constructed predicate
     */
    public static <M> Predicate buildPredicate(
            EntityManager em,
            Root<M> root,
            OmniSearchBaseOptions options
    ) {
        return buildPredicate(em, root, options, BuilderTools.DEFAULT);
    }

    /**
     * Builds a {@link Predicate} based on provided options using a specific builder tools instance.
     *
     * @param em           the entity manager
     * @param root         the root path of the query
     * @param options      the search options
     * @param builderTools tools to support RSQL parsing and path resolution
     * @param <M>          the entity type
     * @return the constructed predicate
     */
    public static <M> Predicate buildPredicate(
            EntityManager em,
            Root<M> root,
            OmniSearchBaseOptions options,
            BuilderTools builderTools
    ) {
        var cb = em.getCriteriaBuilder();
        var predicate = cb.conjunction();

        var search = options.getSearch();
        if (search != null && !search.isBlank()) {
            predicate = searchInAllColumns(search, root, em, options.getJoins());
        }

        var conditions = options.getQuery();
        if (conditions != null) {
            var visitor = new JpaPredicateVisitor<>(root, builderTools);
            var filtersPredicate = conditions.accept(visitor, new EntityManagerAdapter(em));
            predicate = cb.and(predicate, filtersPredicate);
        }

        return predicate;
    }

    /**
     * Container record holding the components needed to execute a JPA criteria query.
     *
     * @param criteriaBuilder the JPA {@link CriteriaBuilder}
     * @param criteriaQuery   the JPA {@link CriteriaQuery}
     * @param root            the root entity of the query
     * @param <Q>             the result type of the query
     * @param <M>             the entity type queried
     */
    public record SearchQuery<Q, M>(CriteriaBuilder criteriaBuilder, CriteriaQuery<Q> criteriaQuery, Root<M> root) {
    }
}
