package com.example.demo.aggregator;

import java.util.Arrays;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Employee;
import com.example.demo.repository.EmployeeRepo;
import com.example.demo.response.ResponseMessage;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@Service
public class EmployeeService {
	@Autowired
	private EmployeeRepo repo;
	
	//save or update one employee method and its fallback method
	@Transactional
	@HystrixCommand(fallbackMethod = "getDataFallBackSaveOrUpdateEmployee")
	public Employee saveOrUpdateEmployee(Employee employee) {
		Employee emp= repo.save(employee);
		return emp;
	}
	public Employee getDataFallBackSaveOrUpdateEmployee(Employee employee) {
		Employee emp= buildFallbackEmployee();
		return emp;
	}
	
	//get all employee method and its fallback method
	@Transactional
	@HystrixCommand(fallbackMethod = "getDataFallBackAllEmployee")
	public List<Employee> getAllEmployee(){
		return repo.findAll();
	}
	public List<Employee> getDataFallBackAllEmployee() {
		Employee emp = new Employee();
		emp.setName("fallback-emp1");
		emp.setDesignation("fallback-manager");
		emp.set_id(ObjectId.get());
		emp.setEmail("fallback-email");
		emp.setPhone("fallback-phone");
		return Arrays.asList(emp);
	}
	
	//get one employee by its id method and its fallback method
	@Transactional
	@HystrixCommand(fallbackMethod = "getDataFallBackFindEmployee")
	public Employee findEmployeeById(ObjectId id) throws IllegalArgumentException{
		return repo.findBy_id(id);
	}
	public Employee getDataFallBackFindEmployee(ObjectId id)  {
		Employee emp= buildFallbackEmployee();
		return emp;
	}
	
	//delete one employee
	@Transactional
	public ResponseEntity<ResponseMessage> deleteEmployee(ObjectId id) {
		try {
			Employee emp=repo.findBy_id(id);
			repo.delete(emp);
			return new ResponseEntity<>(new ResponseMessage("Successfully deleted"),HttpStatus.OK);
		}
		catch(Exception e) {
			return new ResponseEntity<>(new ResponseMessage("Deletion can not be performed"),HttpStatus.CONFLICT);
		}	
	}
	
	//build a fallback employee
	private Employee buildFallbackEmployee() {
		Employee emp = new Employee();
		emp.setName("fallback-emp1");
		emp.setDesignation("fallback-manager");
		emp.set_id(ObjectId.get());
		emp.setEmail("fallback-email");
		emp.setPhone("fallback-phone");
		return emp;
	}
	public ResponseEntity<ResponseMessage> UserNameExistedOrNot(String username) {
		Employee emp=repo.findByUsername(username);
		return this.resourceUsedOrNot(emp);
	}
	public ResponseEntity<ResponseMessage> emailExistedOrNot(String email) {
		Employee emp=repo.findByEmail(email);
		return this.resourceUsedOrNot(emp);
		
	}
	public ResponseEntity<ResponseMessage> phoneExistedOrNot(String phone) {
		Employee emp=repo.findByPhone(phone);
		return this.resourceUsedOrNot(emp);
	}
	private ResponseEntity<ResponseMessage> resourceUsedOrNot(Employee emp){
		if(null!=emp) {
			return new ResponseEntity<>(new ResponseMessage("Already in use"),HttpStatus.CONFLICT);
		}
		else {
			return new ResponseEntity<>(new ResponseMessage("Not Used"),HttpStatus.OK);
		}
	}

}