package com.example.demo.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseMessage {
	@JsonProperty("message")
	private String message;
	public ResponseMessage(String message){
		this.message=message;
	}
	ResponseMessage(){
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	@Override
	public String toString() {
		return "ResponseMessage [message=" + message + "]";
	}
	
}