package com.example.demo.dao;



import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Employee;
import com.example.demo.model.EmployeeInfo;

@Repository
public class EmployeeDAOHibernateImpl implements EmployeeDao {

	private EntityManager entityManager;
	@Autowired
	public EmployeeDAOHibernateImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public Employee findEmployeeById(int id) {
		Session currentSession=entityManager.unwrap(Session.class);
		Employee employee=currentSession.get(Employee.class,id);
		return employee;
	}

	@Override
	public void saveEmployee(Employee employee) {
		Session currentSession=entityManager.unwrap(Session.class);
		EmployeeInfo employeeInfo=new EmployeeInfo(employee.getEmail());
		employee.setEmployeeInfo(employeeInfo);
		currentSession.saveOrUpdate(employee);
		
	}

	@Override
	public void deleteEmployeeById(int theId) {
		Session currentSession=entityManager.unwrap(Session.class);
		Query query=currentSession.createQuery("delete from Employee where id=:employeeId");
		query.setParameter("employeeId", theId);
		query.executeUpdate();
		
	}
	@Override
	public List<EmployeeInfo> findAllEmployeeInfo() {
		Session currentSession=entityManager.unwrap(Session.class);
		Query<EmployeeInfo> query=currentSession.createQuery("from EmployeeInfo",EmployeeInfo.class);
		List<EmployeeInfo> employees=query.getResultList();
		return employees;
	}

	@Override
	public void updateEmployeeInfo(EmployeeInfo employeeInfo) {
		Session currentSession=entityManager.unwrap(Session.class);
		currentSession.saveOrUpdate(employeeInfo);
		
	}

	@Override
	public List<EmployeeInfo> searchEmployeeInfo(String key) {
		Session currentSession=entityManager.unwrap(Session.class);
		Query<EmployeeInfo> query=currentSession.createQuery("from EmployeeInfo where first_name=:key or designation=:key",EmployeeInfo.class);
		query.setParameter("key", key);
		List<EmployeeInfo> employeeInfos=query.getResultList();
		return employeeInfos;
	}
	@Override
	public Employee findEmployeeByEmail(String email) {
		Session currentSession=entityManager.unwrap(Session.class);
		Query<Employee> query=currentSession.createQuery("from Employee where email=:email",Employee.class);
		query.setParameter("email", email);
		List<Employee> employees=query.getResultList();
		if(employees.size()==0) {
			return null;
		}
		Employee employee=employees.get(0);
		return employee;
	}

	@Override
	public void updateEmployee(Employee employee) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public EmployeeInfo findEmployeeInfoById(int id) {
		Session currentSession=entityManager.unwrap(Session.class);
		EmployeeInfo employee=currentSession.get(EmployeeInfo.class,id);
		return employee;
	}

}
