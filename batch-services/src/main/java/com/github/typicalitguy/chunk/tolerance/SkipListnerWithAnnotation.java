package com.github.typicalitguy.chunk.tolerance;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.Date;

import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.annotation.OnSkipInRead;
import org.springframework.batch.core.annotation.OnSkipInWrite;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import com.github.typicalitguy.chunk.reader.csv.StudentCSV;
import com.github.typicalitguy.chunk.reader.json.StudentJSON;

@Component
public class SkipListnerWithAnnotation {

	@OnSkipInRead
	public void skipInRead(Throwable throwable) {
		if (throwable instanceof FlatFileParseException) {
			createFile(path("/fault-tolerant-logs/reader/reader-error.log"),
					((FlatFileParseException) throwable).getInput() + "|FlatFileParseException");
		}
	}

	@OnSkipInProcess
	public void skipInProcess(StudentCSV student, Throwable throwable) {
		if (throwable instanceof NullPointerException) {
			createFile(path("/fault-tolerant-logs/processor/processor-error.log"),
					student.toString() + "|NullPointerException");
		}
	}

	@OnSkipInWrite
	public void skipInWriter(StudentJSON student, Throwable throwable) {
		if (throwable instanceof RuntimeException) {
			createFile(path("/fault-tolerant-logs/writer/writer-error.log"),
					student.toString() + "|RuntimeException");
		}
	}
	public static void createFile(String path, String data) {
		try (FileWriter fileWriter = new FileWriter(new File(path), true)) {
			fileWriter.write(data + "|" + new Date() + "\n");
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	private static String path(String path) {
		return Paths.get(System.getProperty("user.dir"), path).toString();
	}
}
