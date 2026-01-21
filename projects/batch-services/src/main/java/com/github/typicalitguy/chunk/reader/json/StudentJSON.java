package com.github.typicalitguy.chunk.reader.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentJSON {
	private String id;
	
	@JsonProperty("first_name")
	private String firstName;
	@JsonProperty("last_name")
	private String lastName;
	private String email;

	 
	public StudentJSON() {
	}

	public StudentJSON(String id, String firstName, String lastName, String email) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String toString() {
		return "StudentCSV [id=" + id + ", firstName=" + firstName + ", lastName=" + lastName + ", email=" + email
				+ "]";
	}

}
