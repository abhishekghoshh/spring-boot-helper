package com.example.demo.service;

import java.util.List;

import com.example.demo.model.Employee;
import com.example.demo.model.EmployeeInfo;

public interface EmployeeService {

	public Employee findEmployeeById(int Id);
	public void saveEmployee(Employee employee);
	public void deleteEmployeeById(int theId);
	public String loginEmployee(Employee employee);
	public void updateEmployeeInfo(EmployeeInfo employeeInfo);
	public List<EmployeeInfo> searchEmployeeInfo(String key);
	public void updateEmployee(Employee employee);
	public Employee findEmployeeByEmail(String email);
	public List<EmployeeInfo> findAllEmployeeInfo();
	public EmployeeInfo findEmployeeInfoById(int id);
	
}
