package com.peluware.omnisearch.jpa.entities;

import com.peluware.omnisearch.EnumSearchCandidate;
import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "user_test")
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
    private List<House> houses = new ArrayList<>();

    @CollectionTable
    @ElementCollection
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private Set<Contacts> contacts = new HashSet<>();

    public enum Level implements EnumSearchCandidate {
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

    public enum Role implements EnumSearchCandidate {
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

    // Getters and Setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public List<House> getHouses() {
        return houses;
    }

    public void setHouses(List<House> houses) {
        this.houses = houses;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Set<Contacts> getContacts() {
        return contacts;
    }

    public void setContacts(Set<Contacts> contacts) {
        this.contacts = contacts;
    }


    // toString, equals, hashCode


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return active == user.active && Objects.equals(id, user.id) && Objects.equals(name, user.name) && Objects.equals(email, user.email) && level == user.level && Objects.equals(houses, user.houses) && Objects.equals(roles, user.roles) && Objects.equals(contacts, user.contacts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, active, level, houses, roles, contacts);
    }

    // BUILDER

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final User user;

        public Builder() {
            this.user = new User();
        }

        public Builder id(Long id) {
            user.setId(id);
            return this;
        }

        public Builder name(String name) {
            user.setName(name);
            return this;
        }

        public Builder email(String email) {
            user.setEmail(email);
            return this;
        }

        public Builder active(boolean active) {
            user.setActive(active);
            return this;
        }

        public Builder level(Level level) {
            user.setLevel(level);
            return this;
        }

        public Builder houses(List<House> houses) {
            user.setHouses(houses);
            return this;
        }

        public Builder roles(Set<Role> roles) {
            user.setRoles(roles);
            return this;
        }

        public Builder contacts(Set<Contacts> contacts) {
            user.setContacts(contacts);
            return this;
        }

        public User build() {
            return user;
        }
    }

}
