package com.example.demo.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Employee;
import com.example.demo.model.EmployeeInfo;
import com.example.demo.security.Authentication;
import com.example.demo.service.EmployeeService;

@RestController
@RequestMapping("/info")
public class EmployeeInfoController {

	@Autowired
	private EmployeeService employeeService;

	@PutMapping("/updateEmployeeInfo")
	public ResponseEntity<String> updateEmployeeInfo(@RequestBody EmployeeInfo employeeInfo,HttpServletRequest httpServletRequest) {
		String token = httpServletRequest.getHeader("Authorization");
		Employee employee=Authentication.validateEmployee(token);
		if(employee!=null) {
			employeeService.updateEmployeeInfo(employeeInfo);
			return new ResponseEntity<String>("Successfull updation",HttpStatus.OK);
		}else {
			throw new RuntimeException("Unauthorized Access");
		}
	}
	@GetMapping("/searchEmployeeInfo/{key}")
	public List<EmployeeInfo> searchEmployeeInfo(@PathVariable String key,HttpServletRequest httpServletRequest) {
		String token = httpServletRequest.getHeader("Authorization");
		Employee employee=Authentication.validateEmployee(token);
		if(employee==null) {
			throw new RuntimeException("Unauthorized Access");
		}
		return employeeService.searchEmployeeInfo(key);
	}
	@GetMapping("/findAllEmployeeInfo")
	public List<EmployeeInfo> findAllEmployeeInfo(HttpServletRequest httpServletRequest) {
		System.out.println("Find all employee info");
		String token = httpServletRequest.getHeader("Authorization");
		Employee employee=Authentication.validateEmployee(token);
		if(employee==null) {
			throw new RuntimeException("Unauthorized Access");
		}
		return employeeService.findAllEmployeeInfo();
	}
	@PutMapping("/update")
	public Employee updateEmployee(@RequestBody Employee employee) {
		employeeService.saveEmployee(employee);
		return employee;
	}
	@DeleteMapping("/delete/{employeeId}")
	public String deleteEmployee(@PathVariable int employeeId) {
		Employee employee = employeeService.findEmployeeById(employeeId);
		if(employee==null) {
			throw new RuntimeException("Employee not found with id - "+employeeId);
		}
		employeeService.deleteEmployeeById(employeeId);
		return "Employee deleted with id - "+employeeId;
	}
	
	@GetMapping("/logout")
	public void logout(HttpServletRequest httpServletRequest) {
		String token = httpServletRequest.getHeader("Authorization");
		Employee employee=Authentication.validateEmployee(token);
		if(employee==null) {
			throw new RuntimeException("Unauthorized Access");
		}
		Authentication.clearSession();
		
	}
	@GetMapping("/findEmployeeInfoById/{id}")
	public EmployeeInfo findEmployeeInfoById(@PathVariable int id,HttpServletRequest httpServletRequest) {
		String token = httpServletRequest.getHeader("Authorization");
		System.out.println("Token is "+token);
		Employee employee=Authentication.validateEmployee(token);
		if(employee==null) {
			throw new RuntimeException("Unauthorized Access");
		}
		return employeeService.findEmployeeInfoById(id);
	}
}
