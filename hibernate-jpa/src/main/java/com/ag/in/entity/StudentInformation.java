package com.ag.in.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_information")
@NamedQuery(name = "find_all_students_with_dept", query = "select st from StudentInformation st")
//@Data for JPA entities is not recommended, I can cause performance and memory consumption issues
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentInformation {
    @Id
    @GeneratedValue
    private long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @OneToOne(fetch = FetchType.LAZY)
    private Passport passport;

    @Column(name = "is_active")
    private int isActive;

    public StudentInformation(String firstName, String lastName, String email, Passport passport) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passport = passport;
        this.isActive = 0;
    }
}
