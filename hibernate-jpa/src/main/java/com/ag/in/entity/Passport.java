package com.ag.in.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Passport {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "passport")
    private StudentInformation student;

    public Passport(String number) {
        this.number = number;
    }
}
