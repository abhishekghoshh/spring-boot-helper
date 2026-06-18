package com.example.demo.controller;



import java.io.IOException;

import org.apache.tomcat.util.json.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Employee;
import com.example.demo.model.ResponseMessage;
import com.example.demo.security.Authentication;
import com.example.demo.service.EmployeeService;


@RestController
@RequestMapping("/api")
public class EmployeeRestController {
	private EmployeeService employeeService;
	@Autowired
	private ResponseMessage response;
	
	@Autowired
	public EmployeeRestController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	@GetMapping("/employees/{employeeId}")
	public Employee getEmployeeById(@PathVariable int employeeId) {
		
		Employee employee=employeeService.findEmployeeById(employeeId);
		if(employee==null) {
			throw new RuntimeException("Employee id not found - "+employeeId);
		}
		return employee;
	}
	
	@PostMapping("/signup")
	public ResponseMessage signupEmployee(@RequestBody Employee employee) throws ParseException, IOException {
		if((employeeService.findEmployeeByEmail(employee.getEmail()))!=null) {
			throw new RuntimeException("email already existed");
		}
		employee.setId(0);
		employeeService.saveEmployee(employee);
		response.setMessage(Authentication.generateToken(employee));
		
		return response;
	}
	@PostMapping("/login")
	public String loginEmployee(@RequestBody Employee employee) {
		return employeeService.loginEmployee(employee);
	}
	@GetMapping("/checkLoggedin")
	public Employee checkLoggedin() {
		Employee employee=Authentication.isLoggedIn();
		return employee;
	}
	@GetMapping("/getToken")
	public String getToken() {
		String token=Authentication.getToken();
		return token;
	}

}

