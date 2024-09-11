package com.github.typicalitguy.chunk.writer.xml;

import java.nio.file.Paths;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.github.typicalitguy.chunk.reader.jdbc.StudentJDBC;
import com.github.typicalitguy.chunk.reader.xml.StudentXML;

@Configuration
public class XmlWriterChunckOriententedJobConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	
	@Autowired
	@Qualifier("mysqlUniversityDatasource")
	private DataSource universitydataSource;
	
//	@Bean
	public Job xmlItemWriterChunkOrientedJob() {
		return jobBuilderFactory.get("first XML item writer chunk oriented job")
				.incrementer(new RunIdIncrementer())
				.start(xmlItemChunkOrientedStep())
				.build();
	}

	private Step xmlItemChunkOrientedStep() {
		return stepBuilderFactory.get("first XML item writer chunk oriented step")
				.<StudentJDBC, StudentXML>chunk(3)
				.reader(jdbcItemReader())
				.processor(processor())
				.writer(staxEventItemWriter())
				.build();
	}
	

	private JdbcCursorItemReader<StudentJDBC> jdbcItemReader(){
		JdbcCursorItemReader<StudentJDBC> jdbcItemReader = new JdbcCursorItemReader<>();
		jdbcItemReader.setDataSource(universitydataSource);
		jdbcItemReader.setSql("select * from students");
		jdbcItemReader.setRowMapper(rowMapper());
		return jdbcItemReader;
	}
	private RowMapper<StudentJDBC> rowMapper() {
		BeanPropertyRowMapper<StudentJDBC> rowMapper = new BeanPropertyRowMapper<>();
		rowMapper.setMappedClass(StudentJDBC.class);
		return rowMapper;
	}
	private StaxEventItemWriter<StudentXML> staxEventItemWriter(){
		StaxEventItemWriter<StudentXML> staxEventItemWriter = new StaxEventItemWriter<>();
		staxEventItemWriter.setResource(absoluteResourcePath("/output-files/xml/students.xml"));
		staxEventItemWriter.setRootTagName("students");
		staxEventItemWriter.setMarshaller(jaxb2Marshaller());
		return staxEventItemWriter;
	}

	private Jaxb2Marshaller jaxb2Marshaller() {
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setClassesToBeBound(StudentXML.class);
		return jaxb2Marshaller;
	}

	private FileSystemResource absoluteResourcePath(String path) {
		return new FileSystemResource(Paths
				.get(System.getProperty("user.dir"), path).toString());
	}
	private ItemProcessor<StudentJDBC,StudentXML> processor() {
		return  studentJDBC ->{
				StudentXML studentXML = new StudentXML();
				studentXML.setId(studentJDBC.getId());
				studentXML.setFirstName(studentJDBC.getFirstName());
				studentXML.setLastName(studentJDBC.getLastName());
				studentXML.setEmail(studentJDBC.getEmail());
				return studentXML;
			};
	}
}
