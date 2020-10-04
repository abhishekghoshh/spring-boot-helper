package com.example.demo.security;

import java.util.HashMap;

import com.example.demo.model.Employee;

public class Authentication {
	private static HashMap<String,String> map=new HashMap<String,String>();
	public static String generateToken(Employee employee) {
		String token="Token."+employee.getId()+"."+employee.getEmail();;
		if(map.isEmpty()||map.get("token").equals(token)) {
			map.put("token", token);
		}
		else {
			throw new RuntimeException("Someone is already logged in");
		}
		System.out.println(map.get("token"));
		return token;
	}
	public static Employee validateEmployee(String token) {
		Employee employee;
		if(map.isEmpty()) {
			employee=null;
		}else if((map.get("token")).equals(token)) {
			int id=Integer.parseInt(String.valueOf(token.charAt(6)));
			String email=token.substring(8);
			employee=new Employee();
			employee.setEmail(email);
			employee.setId(id);
		}else {
			employee=null;
		}
		return employee;
	}
	public static void clearSession() {
		map.clear();
	}
	public static Employee isLoggedIn() {
		Employee employee;
		if(map.isEmpty()) {
			employee=null;
		}else {
			int id=Integer.parseInt(String.valueOf(map.get("token").charAt(6)));
			String email=map.get("token").substring(8);
			employee=new Employee();
			employee.setEmail(email);
			employee.setId(id);
		}
		return employee;
	}

	public static String getToken() {
		return map.get("token");
	}
}
