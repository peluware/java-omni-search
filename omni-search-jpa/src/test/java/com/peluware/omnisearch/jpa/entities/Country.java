package com.peluware.omnisearch.jpa.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "country_test")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Country {

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "continent_code", nullable = false)
    private Countinent countinent;

}
