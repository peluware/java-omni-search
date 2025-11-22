package com.peluware.omnisearch.jpa.entities;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "house_test")
public class House {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private int numberOfRooms;

    @ManyToOne
    @JoinColumn(name = "country_code", nullable = false)
    private Country country;

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getNumberOfRooms() {
        return numberOfRooms;
    }

    public void setNumberOfRooms(int numberOfRooms) {
        this.numberOfRooms = numberOfRooms;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        House house = (House) o;
        return numberOfRooms == house.numberOfRooms && Objects.equals(id, house.id) && Objects.equals(name, house.name) && Objects.equals(address, house.address) && Objects.equals(country, house.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, address, numberOfRooms, country);
    }

    //BUILDER

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final House house;

        public Builder() {
            this.house = new House();
        }

        public Builder id(Long id) {
            house.setId(id);
            return this;
        }

        public Builder name(String name) {
            house.setName(name);
            return this;
        }

        public Builder address(String address) {
            house.setAddress(address);
            return this;
        }

        public Builder numberOfRooms(int numberOfRooms) {
            house.setNumberOfRooms(numberOfRooms);
            return this;
        }

        public Builder country(Country country) {
            house.setCountry(country);
            return this;
        }

        public House build() {
            return house;
        }
    }
}
