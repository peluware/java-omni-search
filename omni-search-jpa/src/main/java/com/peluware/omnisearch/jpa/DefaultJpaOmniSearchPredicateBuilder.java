package com.peluware.omnisearch.jpa;


import com.peluware.omnisearch.core.EnumSearchCandidate;
import com.peluware.omnisearch.core.OmniSearchBaseOptions;
import com.peluware.omnisearch.core.ParseNumber;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Year;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Defatulr implementation of {@link JpaOmniSearchPredicateBuilder} responsible for building JPA Criteria {@link Predicate} objects
 * based on {@link OmniSearchBaseOptions}, supporting search across all columns,
 * embedded fields, element collections, and associations.
 *
 * <p>It supports filtering using RSQL nodes and handles special types like UUID, numbers,
 * booleans, enums, and {@link java.time.Year}.</p>
 */
@Slf4j
public class DefaultJpaOmniSearchPredicateBuilder implements JpaOmniSearchPredicateBuilder {

    /**
     * Pattern used to detect UUID values in string form.
     */
    protected static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

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
    protected <E> Predicate searchInAllColumns(
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
    @SuppressWarnings("java:S3776")
    protected Collection<Predicate> getSearchPredicates(String search, Path<?> path, EntityManager em) {
        var javaType = path.getJavaType();
        var managedType = em.getMetamodel().managedType(javaType);
        var predicates = new ArrayList<Predicate>();

        for (var attribute : managedType.getAttributes()) {
            try {

                var propertyName = attribute.getName();
                var type = attribute.getPersistentAttributeType();

                if (type == Attribute.PersistentAttributeType.BASIC) {
                    var basicPredicate = getBasicPredicates(search, path.get(propertyName), em);
                    if (basicPredicate != null) {
                        predicates.add(basicPredicate);
                    }
                } else if (type == Attribute.PersistentAttributeType.EMBEDDED) {
                    predicates.addAll(getSearchPredicates(search, path.get(propertyName), em));
                } else if (type == Attribute.PersistentAttributeType.ELEMENT_COLLECTION &&
                        attribute instanceof PluralAttribute<?, ?, ?> plural &&
                        path instanceof From<?, ?> from &&
                        plural.getElementType().getPersistenceType() == Type.PersistenceType.BASIC) {
                    var basicPredicate = getBasicPredicates(search, from.join(propertyName, JoinType.LEFT), em);
                    if (basicPredicate != null) {
                        predicates.add(basicPredicate);
                    }
                }
            } catch (IllegalArgumentException e) {
                log.trace("Could not parse search value '{}' for attribute '{}': {}", search, attribute.getName(), e.getMessage());
            } catch (Exception e) {
                log.trace("Could not create predicate for attribute '{}': {}", attribute.getName(), e.getMessage());
            }

        }

        return predicates;
    }


    /**
     * Builds predicates for simple types like String, UUID, Number, Boolean, Year, and Enums.
     *
     * @param search the search term
     * @param path   the path to the attribute
     * @param em     the entity manager
     * @return a collection of predicates
     */
    @SuppressWarnings("java:S3776")
    protected @Nullable Predicate getBasicPredicates(String search, Path<?> path, EntityManager em) {
        var type = path.getJavaType();
        var cb = em.getCriteriaBuilder();

        if (String.class.isAssignableFrom(type)) {
            return cb.like(cb.lower(path.as(String.class)), "%" + search.toLowerCase() + "%");
        }

        if (UUID.class.isAssignableFrom(type) && UUID_PATTERN.matcher(search).matches()) {
            return cb.equal(path, UUID.fromString(search));
        }


        if ((Boolean.class.isAssignableFrom(type) || type == boolean.class) && search.matches("true|false")) {
            return cb.equal(path, Boolean.parseBoolean(search));
        }

        if (Year.class.isAssignableFrom(type) && search.matches("\\d{4}")) {
            return cb.equal(path, Year.parse(search));
        }

        if (type.isEnum()) {
            @SuppressWarnings("unchecked")
            var candidates = EnumSearchCandidate.collectEnumCandidates((Class<? extends Enum<?>>) type, search);
            if (candidates.isEmpty()) {
                return null;
            }
            return path.in(candidates);
        }


        if ((Number.class.isAssignableFrom(type) || type.isPrimitive()) && search.matches("[+-]?\\d*\\.?\\d+")) {
            for (var parser : ParseNumber.PARSERS) {
                if (type.isAssignableFrom(parser.type())) {
                    return cb.equal(path, parser.parse(search));
                }
            }
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public <M> Predicate buildPredicate(EntityManager em, Root<M> root, OmniSearchBaseOptions options) {
        var cb = em.getCriteriaBuilder();
        var predicate = cb.conjunction();

        var search = options.getSearch();
        if (search != null && !search.isBlank()) {
            predicate = searchInAllColumns(search, root, em, options.getPropagations());
        }

        return predicate;
    }
}
