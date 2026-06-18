package com.example.demo.entity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

@Document(collection="employee")
public class Employee {
	@Id
	private ObjectId _id;
	@NotNull
	private String username;
	@NotNull
	private String password;
	@NotNull
	private String name;
	@NotNull
	private String email;
	@NotNull
	private String designation;
	@NotNull
	private String phone;
	
	public Employee(){	
	}
	
	public Employee(ObjectId _id, @NotNull String username, @NotNull String password, @NotNull String name,
			@NotNull String email, @NotNull String designation, @NotNull String phone) {
		super();
		this._id = _id;
		this.username = username;
		this.password = password;
		this.name = name;
		this.email = email;
		this.designation = designation;
		this.phone = phone;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String get_id() {
		return _id.toHexString();
	}
	public void set_id(ObjectId _id) {
		this._id = _id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getDesignation() {
		return designation;
	}
	public void setDesignation(String designation) {
		this.designation = designation;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	
}
