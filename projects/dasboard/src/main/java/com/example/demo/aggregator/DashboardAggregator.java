package com.example.demo.aggregator;

import java.io.IOException;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
//import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.demo.entity.Employee;
import com.example.demo.response.ResponseMessage;


@Service
public class DashboardAggregator {
	@Autowired
	private RestTemplate restTemplate;
//	private DiscoveryClient discoveryClient;
	private LoadBalancerClient loadBalancer;
	private String url;
	
	@Autowired
	public DashboardAggregator(/*DiscoveryClient discoveryClient*/ LoadBalancerClient loadBalancer) throws RestClientException, IOException{
		try {
			
			this.loadBalancer=loadBalancer;
			ServiceInstance serviceInstance=this.loadBalancer.choose("db-operation");
			url=serviceInstance.getUri().toString();
			
//			this.discoveryClient=discoveryClient;
//			List<ServiceInstance> instances=this.discoveryClient.getInstances("db-operation");
//			ServiceInstance instance = instances.get(0);
//			url=instance.getUri().toString();
			url=url+"/employees";
		}
	catch(Exception e) {
		System.out.println(e);
	}
		
		
	}
	
	public Employee addEmployee(Employee employee) {
		employee.set_id(ObjectId.get());
		ResponseEntity<Employee> response = restTemplate.postForEntity(url+"/save", employee, Employee.class);
        employee = response.getBody();
        return employee;
	}
	public Employee findEmployeeById(ObjectId id) {
		ResponseEntity<Employee> response = restTemplate.getForEntity(url+"/find/"+id, Employee.class);
        Employee employee = response.getBody();
        return employee;
	}
	public ResponseEntity<ResponseMessage> deleteEmployee(ObjectId id) {
		
		try {
			ResponseEntity<ResponseMessage> response=restTemplate.exchange(url+"/delete/"+id,HttpMethod.DELETE,null,ResponseMessage.class);
			System.out.println("Hi"+response.getBody().toString());
			return response;
		}
		catch(Exception e) {
			System.out.println("Error is "+e);
		}
		return null;
		
	}
	public Employee updateEmployee(Employee employee) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");
		HttpEntity<?> httpEntity = new HttpEntity<Object>(employee, headers);
		ResponseEntity<Employee> response = restTemplate.exchange(url+"/update",HttpMethod.PUT,httpEntity,Employee.class);
		employee=response.getBody();
		return employee;
	}
	public List<Employee> getAllEmployee(){
		ResponseEntity<List<Employee>> response = restTemplate.exchange(
					  url,
					  HttpMethod.GET,
					  null,
					  new ParameterizedTypeReference<List<Employee>>(){});
		List<Employee> employees = response.getBody();
		return employees;		
	}
}