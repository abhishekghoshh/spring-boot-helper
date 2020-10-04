package com.example.demo.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.demo.entity.Employee;

@Repository
public interface EmployeeRepo extends MongoRepository<Employee,String> {
	Employee findBy_id(ObjectId _id);
	Employee findByUsername(String username);
	Employee findByEmail(String email);
	Employee findByPhone(String phone);
}