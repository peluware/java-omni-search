package com.peluware.omnisearch.jpa;


import com.peluware.omnisearch.EnumSearchCandidate;
import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.jpa.rsql.DefaultRsqlJpaBuilderOptions;
import com.peluware.omnisearch.jpa.rsql.JpaPredicateVisitor;
import com.peluware.omnisearch.jpa.rsql.RsqlJpaBuilderOptions;
import com.peluware.omnisearch.utils.ParseNumber;
import cz.jirutka.rsql.parser.RSQLParser;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class DefaultJpaOmniSearchPredicateBuilder implements JpaOmniSearchPredicateBuilder {

    /**
     * Pattern used to detect UUID values in string form.
     */
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Logger log = LoggerFactory.getLogger(DefaultJpaOmniSearchPredicateBuilder.class);


    private final RSQLParser rsqlParser;
    private final RsqlJpaBuilderOptions rsqlJpaBuilderOptions;

    public DefaultJpaOmniSearchPredicateBuilder(RSQLParser rsqlParser, RsqlJpaBuilderOptions rsqlJpaBuilderOptions) {
        this.rsqlParser = rsqlParser;
        this.rsqlJpaBuilderOptions = rsqlJpaBuilderOptions;
    }

    public DefaultJpaOmniSearchPredicateBuilder(RSQLParser rsqlParser) {
        this(rsqlParser, new DefaultRsqlJpaBuilderOptions());
    }

    public DefaultJpaOmniSearchPredicateBuilder() {
        this(new RSQLParser());
    }


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
            JpaContext em,
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
     * @param search     the search keyword
     * @param path       the current path to inspect
     * @param jpaContext the JPA context
     * @return a collection of predicates matching the search keyword
     */
    @SuppressWarnings("java:S3776")
    protected Collection<Predicate> getSearchPredicates(String search, Path<?> path, JpaContext jpaContext) {
        var javaType = path.getJavaType();
        var managedType = jpaContext.getMetamodel().managedType(javaType);
        var predicates = new ArrayList<Predicate>();

        for (var attribute : managedType.getAttributes()) {
            try {

                var propertyName = attribute.getName();
                var type = attribute.getPersistentAttributeType();

                if (type == Attribute.PersistentAttributeType.BASIC) {

                    var basicPredicate = getBasicPredicates(
                            search,
                            path.get(propertyName),
                            jpaContext
                    );

                    if (basicPredicate != null) {
                        predicates.add(basicPredicate);
                    }
                } else if (type == Attribute.PersistentAttributeType.EMBEDDED) {

                    var embeddedPredicates = getSearchPredicates(
                            search,
                            path.get(propertyName),
                            jpaContext
                    );

                    predicates.addAll(embeddedPredicates);
                } else if (type == Attribute.PersistentAttributeType.ELEMENT_COLLECTION &&
                        attribute instanceof PluralAttribute<?, ?, ?> plural &&
                        path instanceof From<?, ?> from &&
                        plural.getElementType().getPersistenceType() == Type.PersistenceType.BASIC) {

                    var basicPredicate = getBasicPredicates(
                            search,
                            from.join(propertyName, JoinType.LEFT),
                            jpaContext
                    );

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
    protected @Nullable Predicate getBasicPredicates(String search, Path<?> path, JpaContext em) {
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
    @Override
    public <M> Predicate buildPredicate(JpaContext jpaContext, Root<M> root, OmniSearchBaseOptions options) {
        var cb = jpaContext.getCriteriaBuilder();
        var predicate = cb.conjunction();

        var search = options.getSearch();
        if (search != null && !search.isBlank()) {
            predicate = searchInAllColumns(search, root, jpaContext, options.getPropagations());
        }

        var query = options.getQuery();
        if (query != null) {
            var node = rsqlParser.parse(query);
            var visitor = new JpaPredicateVisitor<>(root, rsqlJpaBuilderOptions);
            var queryPredicates = node.accept(visitor, jpaContext);
            predicate = cb.and(predicate, queryPredicates);
        }

        return predicate;
    }
}
