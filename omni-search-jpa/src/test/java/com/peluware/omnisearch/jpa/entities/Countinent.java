package com.peluware.omnisearch.jpa.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "countinent_test")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Countinent {
    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;
}
