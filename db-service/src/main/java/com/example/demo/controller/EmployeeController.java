package com.example.demo.controller;

import java.util.List;

import javax.validation.Valid;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.aggregator.EmployeeService;
import com.example.demo.entity.Employee;
import com.example.demo.repository.EmployeeRepo;
import com.example.demo.response.ResponseMessage;

@RestController
@RequestMapping("/employees")
public class EmployeeController {
	
	@Autowired
	EmployeeService service;
	
	@Autowired
	EmployeeRepo repo;
	
	@GetMapping("")
	public List<Employee> getAllEmployee(){
		return service.getAllEmployee();
	}
	@GetMapping("/find/{id}")
	public Employee findEmployeeById(@PathVariable("id") ObjectId id){
		return service.findEmployeeById(id);
	}
		
	@PutMapping("/update")
	public Employee updateEmployee(@Valid @RequestBody Employee employee) {
			return service.saveOrUpdateEmployee(employee);
		}
	@PostMapping("/save")
	public Employee addEmployee(@Valid @RequestBody Employee employee) {
		System.out.println("here also called");
		return service.saveOrUpdateEmployee(employee);
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<ResponseMessage> deleteEmployee(@PathVariable ObjectId id) {
		return service.deleteEmployee(id);
	}	
	@GetMapping("/findUserName/{username}")
	public ResponseEntity<ResponseMessage> findUsername(@PathVariable String username) {
		return service.UserNameExistedOrNot(username);
	}
	@GetMapping("/findEmail/{email}")
	public ResponseEntity<ResponseMessage> findEmail(@PathVariable String email) {
		return service.emailExistedOrNot(email);
	}
	@GetMapping("/findPhone/{phone}")
	public ResponseEntity<ResponseMessage> findPhone(@PathVariable String phone) {
		return service.phoneExistedOrNot(phone);
	}
	
	

}
