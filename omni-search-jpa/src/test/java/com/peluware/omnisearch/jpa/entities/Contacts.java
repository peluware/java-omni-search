package com.peluware.omnisearch.jpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "contacts")
public class Contacts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Contacts contacts = (Contacts) o;
        return Objects.equals(id, contacts.id) && Objects.equals(firstName, contacts.firstName) && Objects.equals(lastName, contacts.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName);
    }

    // BUILDER
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Contacts contacts;

        public Builder() {
            this.contacts = new Contacts();
        }

        public Builder id(Long id) {
            contacts.setId(id);
            return this;
        }

        public Builder firstName(String firstName) {
            contacts.setFirstName(firstName);
            return this;
        }

        public Builder lastName(String lastName) {
            contacts.setLastName(lastName);
            return this;
        }

        public Contacts build() {
            return contacts;
        }
    }
}
