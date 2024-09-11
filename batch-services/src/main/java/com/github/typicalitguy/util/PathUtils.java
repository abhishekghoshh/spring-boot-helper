package com.github.typicalitguy.util;

import java.nio.file.Paths;

import org.springframework.core.io.FileSystemResource;

public class PathUtils {
	private PathUtils() {}
	public static FileSystemResource resource(String relativePath) {
		return new FileSystemResource(path(relativePath));
	}

	public static String path(String relativePath) {
		return Paths.get(System.getProperty("user.dir"), relativePath).toString();
	}
}
