package com.peluware.omnisearch.jpa.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "countinent_test")
public class Countinent {
    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Countinent that = (Countinent) o;
        return Objects.equals(code, that.code) && Objects.equals(name, that.name) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, description);
    }

    // BUILDER
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Countinent countinent;

        public Builder() {
            this.countinent = new Countinent();
        }

        public Builder code(String code) {
            countinent.setCode(code);
            return this;
        }

        public Builder name(String name) {
            countinent.setName(name);
            return this;
        }

        public Builder description(String description) {
            countinent.setDescription(description);
            return this;
        }

        public Countinent build() {
            return countinent;
        }
    }
}
