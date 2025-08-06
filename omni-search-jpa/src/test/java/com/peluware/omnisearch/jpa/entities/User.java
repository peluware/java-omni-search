package com.peluware.omnisearch.jpa.entities;

import com.peluware.omnisearch.jpa.JpaEnumSearchCandidate;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Table(name = "user_test")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private boolean active;

    @Enumerated(EnumType.STRING)
    private Level level;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    @Builder.Default
    private List<House> houses = new ArrayList<>();

    @CollectionTable
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    @Builder.Default
    private Set<Contacts> contacts = new HashSet<>();

    public enum Level implements JpaEnumSearchCandidate {
        LOW, MEDIUM, HIGH;

        @Override
        public boolean isCandidate(String value) {
            if (value == null || value.isBlank()) {
                return false;
            }
            try {
                Level level = Level.valueOf(value.trim().toUpperCase());
                return this == level;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    public enum Role implements JpaEnumSearchCandidate {
        USER, ADMIN, GUEST;

        @Override
        public boolean isCandidate(String value) {
            if (value == null || value.isBlank()) {
                return false;
            }
            try {
                Role role = Role.valueOf(value.trim().toUpperCase());
                return this == role;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}
