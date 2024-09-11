package com.ag.in.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student")
@NamedQuery(name = "find_all_students", query = "select st from Student st")
@NamedQueries(value = {})
//@Data for JPA entities is not recommended, I can cause performance and memory consumtion issues
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @Id
    @GeneratedValue
    private long id;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "email")
    private String email;
    @Column(name = "dept_id")
    private int deptId;
    @Column(name = "is_active")
    private int isActive;



    public Student(String firstName, String lastName, String email, int deptId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.deptId = deptId;
        this.isActive = 0;
    }
}
