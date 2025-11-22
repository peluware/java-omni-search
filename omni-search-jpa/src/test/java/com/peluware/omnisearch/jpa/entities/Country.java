package com.peluware.omnisearch.jpa.entities;


import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "country_test")
public class Country {

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "continent_code", nullable = false)
    private Countinent countinent;


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

    public Countinent getCountinent() {
        return countinent;
    }

    public void setCountinent(Countinent countinent) {
        this.countinent = countinent;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Country country = (Country) o;
        return Objects.equals(code, country.code) && Objects.equals(name, country.name) && Objects.equals(countinent, country.countinent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, countinent);
    }

    // BUILDER

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Country country;

        public Builder() {
            this.country = new Country();
        }

        public Builder code(String code) {
            country.setCode(code);
            return this;
        }

        public Builder name(String name) {
            country.setName(name);
            return this;
        }

        public Builder countinent(Countinent countinent) {
            country.setCountinent(countinent);
            return this;
        }

        public Country build() {
            return country;
        }
    }
}
