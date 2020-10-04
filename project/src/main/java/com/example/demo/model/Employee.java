package com.example.demo.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;


@Entity
@Table(name="employee")
public class Employee {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private int id;
	@NotNull
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="email")
	private String email;
	@NotNull
	@Column(name="password")
	private String password;
	@Column(name="firstAns")
	private String firstAns;
	@Column(name="secondAns")
	private String secondAns;
	
	@OneToOne(cascade=CascadeType.ALL)
	@JoinColumn(name="id")
	private EmployeeInfo employeeInfo;
	public Employee() {
		
	}
	
	public Employee(int id, @NotNull String email, @NotNull String password, String firstAns, String secondAns) {
		this.id = id;
		this.email = email;
		this.password = password;
		this.firstAns = firstAns;
		this.secondAns = secondAns;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	public void setEmployeeInfo(EmployeeInfo employeeInfo) {
		this.employeeInfo = employeeInfo;
	}
	public EmployeeInfo getEmployeeInfo() {
		return employeeInfo;
	}

	public String getFirstAns() {
		return firstAns;
	}

	public void setFirstAns(String firstAns) {
		this.firstAns = firstAns;
	}

	public String getSecondAns() {
		return secondAns;
	}

	public void setSecondAns(String secondAns) {
		this.secondAns = secondAns;
	}

	@Override
	public String toString() {
		return "Employee [id=" + id + ", email=" + email + ", password=" + password + ", firstAns=" + firstAns
				+ ", secondAns=" + secondAns + ", employeeInfo=" + employeeInfo + "]";
	}
	
	
	

}
