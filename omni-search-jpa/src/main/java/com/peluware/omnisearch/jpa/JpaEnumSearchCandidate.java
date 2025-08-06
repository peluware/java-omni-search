package com.peluware.omnisearch.jpa;

/**
 * Interface for JPA Enum Search Candidates.
 * Defines methods to retrieve the name and ordinal of the enum,
 * and to check if a given value is a candidate for the enum.
 */
public interface JpaEnumSearchCandidate {

    String name();

    int ordinal();

    boolean isCandidate(String value);
}
