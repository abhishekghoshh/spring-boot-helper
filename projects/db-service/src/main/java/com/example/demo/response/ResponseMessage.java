package com.example.demo.response;

public class ResponseMessage {
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
}
