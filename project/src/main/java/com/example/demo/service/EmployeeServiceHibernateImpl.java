package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.EmployeeDao;
import com.example.demo.model.Employee;
import com.example.demo.model.EmployeeInfo;
import com.example.demo.security.Authentication;

@Service
public class EmployeeServiceHibernateImpl implements EmployeeService {

	private EmployeeDao employeeDao;
	
	@Autowired
	public EmployeeServiceHibernateImpl(EmployeeDao employeeDao) {
		this.employeeDao = employeeDao;
	}

	@Override
	@Transactional
	public Employee findEmployeeById(int Id) {
		return employeeDao.findEmployeeById(Id);
	}

	@Override
	@Transactional
	public void saveEmployee(Employee employee) {
		employeeDao.saveEmployee(employee);
	}

	@Override
	@Transactional
	public void deleteEmployeeById(int theId) {
		employeeDao.deleteEmployeeById(theId);

	}

	@Override
	@Transactional
	public String loginEmployee(Employee employee) {
		int employeeId=employee.getId();
		String email=employee.getEmail();
		Employee tempEmployee=null;
		Employee tempEmployee1=employeeDao.findEmployeeById(employeeId);
		Employee tempEmployee2=employeeDao.findEmployeeByEmail(email);
		if(tempEmployee1==null&&tempEmployee2==null) {
			throw new RuntimeException("Employee not found");
		}
		if(tempEmployee1!=null) {
			tempEmployee=tempEmployee1;
		}
		if(tempEmployee2!=null) {
			tempEmployee=tempEmployee2;
		}
		if(!employee.getPassword().equals(tempEmployee.getPassword())) {
			throw new RuntimeException("Password is incorrect");
		}
		return Authentication.generateToken(tempEmployee);
		
	}

	@Override
	@Transactional
	public void updateEmployeeInfo(EmployeeInfo employeeInfo) {
		employeeDao.updateEmployeeInfo(employeeInfo);
	}

	@Override
	@Transactional
	public List<EmployeeInfo> searchEmployeeInfo(String key) {
		return employeeDao.searchEmployeeInfo(key);
	}

	@Override
	public void updateEmployee(Employee employee) {
		// TODO Auto-generated method stub
		
	}

	@Override
	@Transactional
	public Employee findEmployeeByEmail(String email) {
		// TODO Auto-generated method stub
		return employeeDao.findEmployeeByEmail(email);
	}

	@Override
	@Transactional
	public List<EmployeeInfo> findAllEmployeeInfo() {
		// TODO Auto-generated method stub
		return employeeDao.findAllEmployeeInfo();
	}

	@Override
	@Transactional
	public EmployeeInfo findEmployeeInfoById(int id) {
		return employeeDao.findEmployeeInfoById(id);
		
	}

}
