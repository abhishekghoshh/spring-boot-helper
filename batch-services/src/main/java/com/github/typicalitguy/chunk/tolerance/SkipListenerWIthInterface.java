package com.github.typicalitguy.chunk.tolerance;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.Date;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.item.file.FlatFileParseException;

import com.github.typicalitguy.chunk.reader.csv.StudentCSV;
import com.github.typicalitguy.chunk.reader.json.StudentJSON;

public class SkipListenerWIthInterface implements SkipListener<StudentCSV, StudentJSON> {

	@Override
	public void onSkipInRead(Throwable throwable) {
		if (throwable instanceof FlatFileParseException) {
			createFile(path("/fault-tolerant-logs/reader/reader-error.log"),
					((FlatFileParseException) throwable).getInput() + "|FlatFileParseException");
		}
	}

	@Override
	public void onSkipInProcess(StudentCSV student, Throwable throwable) {
		if (throwable instanceof NullPointerException) {
			createFile(path("/fault-tolerant-logs/processor/processor-error.log"),
					student.toString() + "|NullPointerException");
		}
	}

	@Override
	public void onSkipInWrite(StudentJSON student, Throwable throwable) {
		if (throwable instanceof RuntimeException) {
			createFile(path("/fault-tolerant-logs/writer/writer-error.log"), student.toString() + "|RuntimeException");
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
