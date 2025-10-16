package com.peluware.omnisearch;

import java.util.Collection;
import java.util.HashSet;

/**
 * Interface for JPA Enum Search Candidates.
 * Defines methods to retrieve the name and ordinal of the enum,
 * and to check if a given value is a candidate for the enum.
 */
public interface EnumSearchCandidate {

    String name();

    int ordinal();

    boolean isCandidate(String value);

    /**
     * Retrieves a list of matching enum constants based on the search term.
     *
     * @param enumType the enum type
     * @param value    the value to match
     * @return a set of matched enum constants
     */
    static Collection<Enum<?>> collectEnumCandidates(Class<? extends Enum<?>> enumType, String value) {
        var set = new HashSet<Enum<?>>();
        for (var constant : enumType.getEnumConstants()) {
            if (constant.name().toLowerCase().contains(value.toLowerCase()) || (constant instanceof EnumSearchCandidate c && c.isCandidate(value))) {
                set.add(constant);
            }
        }
        return set;
    }

}
