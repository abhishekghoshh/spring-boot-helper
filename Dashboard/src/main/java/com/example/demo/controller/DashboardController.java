package com.example.demo.controller;

import java.util.List;

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

import com.example.demo.aggregator.DashboardAggregator;
import com.example.demo.entity.Employee;
import com.example.demo.response.ResponseMessage;

@RestController
@RequestMapping("/employees")
public class DashboardController {
	@Autowired
	DashboardAggregator aggregator;
	@PostMapping("/save")
	public Employee addEmployee(@RequestBody Employee employee) {
        return aggregator.addEmployee(employee);
	}
	@GetMapping("/find/{id}")
	public Employee findEmployeeById(@PathVariable ObjectId id) {
        return aggregator.findEmployeeById(id);
	}
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<ResponseMessage> deleteEmployee(@PathVariable ObjectId id) {
		return aggregator.deleteEmployee(id);
	}
	@PutMapping("/update")
	public Employee updateEmployee(@RequestBody Employee employee) {
		return aggregator.updateEmployee(employee);
	}
	@GetMapping("")
	public List<Employee> getAllEmployee(){
		System.out.println("getAllEmployee is called");
		return aggregator.getAllEmployee();		
	}
}