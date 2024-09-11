package com.github.typicalitguy.chunk.demo;

import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

public class StudentItemReader implements ItemReader<Student> {

	private List<Student> list = List.of(
			new Student("111111", "John", "Doe", "jdoe@example.com"),
			new Student("111112", "Jane", "Smith", "jsmith@example.com"),
			new Student("111113", "Sarah", "Thomas", "sthomas@example.com"),
			new Student("111114", "Frank", "Brown", "fbrown@example.com"),
			new Student("111115", "Mike", "Davis", "mdavis@example.com"),
			new Student("111116", "Jennifer", "Wilson", "jwilson@example.com"),
			new Student("111117", "Jessica", "Garcia", "jgarcia@example.com"),
			new Student("111118", "Fred", "Clark", "fclark@example.com"),
			new Student("111119", "Bob", "Lopez", "blopez@example.com"));
	int index = 0;

	@Override
	public Student read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		if (index < list.size()) {
			return list.get(index++);
		}
		index = 0;
		return null;
	}

}
