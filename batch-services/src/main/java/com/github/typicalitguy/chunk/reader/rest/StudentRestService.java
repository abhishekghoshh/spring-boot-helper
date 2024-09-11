package com.github.typicalitguy.chunk.reader.rest;

import java.util.LinkedList;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class StudentRestService {

	private LinkedList<StudentRest> students = null;

	public LinkedList<StudentRest> getStudents() {
		return new RestTemplate().exchange("http://127.0.0.1:8001/students", HttpMethod.GET,
				new HttpEntity<>(headers()), new ParameterizedTypeReference<LinkedList<StudentRest>>() {
				}).getBody();
	}

	public StudentRest getStudent(String message) {
		System.out.println(message);
		if (null == students) {
			students = getStudents();
		}
		if (!students.isEmpty()) {
			return students.removeFirst();
		}
		return null;
	}

	public StudentRest addStudent(StudentRest studentRest) {
		return new RestTemplate().exchange("http://127.0.0.1:8001/students", HttpMethod.POST,
				new HttpEntity<>(studentRest, headers()), StudentRest.class).getBody();
	}

	private MultiValueMap<String, String> headers() {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.put("Content-Type", List.of("application/json"));
		headers.put("Accept", List.of("application/json"));
		return headers;
	}
}
