package com.github.typicalitguy.chunk.demo;

import org.springframework.batch.item.ItemProcessor;

import com.github.typicalitguy.logger.Logger;

public class StudentItemProcessor implements ItemProcessor<Student, Student> {

	@Override
	public Student process(Student item) throws Exception {
		Logger.log("Student :"+item.getId() + " is processed");
		return item;
	}

}
