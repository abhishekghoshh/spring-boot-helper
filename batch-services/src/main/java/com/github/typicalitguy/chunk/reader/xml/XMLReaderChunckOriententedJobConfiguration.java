package com.github.typicalitguy.chunk.reader.xml;

import java.nio.file.Paths;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
public class XMLReaderChunckOriententedJobConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

//	@Bean
	public Job xmlReaderChunkOrientedJob() {
		return jobBuilderFactory.get("first XML reader chunk oriented job")
				.incrementer(new RunIdIncrementer())
				.start(firstXMLChunkOrientedStep())
				.build();
	}

	private Step firstXMLChunkOrientedStep() {
		return stepBuilderFactory.get("first XML reader chunk oriented step")
				.<StudentXML, StudentXML>chunk(3)
				.reader(xmlItemReader())
				.writer(itemWriter())
				.build();
	}

	public StaxEventItemReader<StudentXML> xmlItemReader() {
		StaxEventItemReader<StudentXML> staxEventItemReader = new StaxEventItemReader<>();
		staxEventItemReader.setResource(absoluteResourcePath("/input-files/xml/students.xml"));
		staxEventItemReader.setFragmentRootElementName("students");
		staxEventItemReader.setUnmarshaller(unmarshaller());
		return staxEventItemReader;
	}

	private Unmarshaller unmarshaller() {
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setClassesToBeBound(StudentXML.class);
		return jaxb2Marshaller;
	}

	private FileSystemResource absoluteResourcePath(String path) {
		return new FileSystemResource(Paths
				.get(System.getProperty("user.dir"), path).toString());
	}


	private ItemWriter<StudentXML> itemWriter() {
		return new ItemWriter<>() {

			@Override
			public void write(List<? extends StudentXML> items) throws Exception {
				System.out.println(items);
			}

		};
	}
}
