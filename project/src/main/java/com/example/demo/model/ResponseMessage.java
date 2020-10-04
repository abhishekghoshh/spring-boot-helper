package com.example.demo.model;

import org.springframework.stereotype.Component;

@Component
public class ResponseMessage {

	private String message;
	public ResponseMessage() {
		
	}
	public ResponseMessage(String message) {
		super();
		this.message = message;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
}
