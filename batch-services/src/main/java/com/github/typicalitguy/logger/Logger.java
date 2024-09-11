package com.github.typicalitguy.logger;

public class Logger {
	public static void log(Object data) {
		System.out.println(null != data ? data.toString() : "null");
	}
}
