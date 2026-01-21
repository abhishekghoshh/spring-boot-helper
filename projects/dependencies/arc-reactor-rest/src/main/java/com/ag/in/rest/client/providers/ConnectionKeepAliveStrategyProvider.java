package com.ag.in.rest.client.providers;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConnectionKeepAliveStrategyProvider {
	private static final long DEFAULT_SECONDS = 30;

	public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
		return (HttpResponse httpResponse, HttpContext context) -> {
			HeaderElementIterator it = new BasicHeaderElementIterator(
					httpResponse.headerIterator(HTTP.CONN_KEEP_ALIVE));
			while (it.hasNext()) {
				HeaderElement headerElement = it.nextElement();
				if (headerElement.getName().equalsIgnoreCase("timeout") && null != headerElement.getValue()) {
					return Long.parseLong(headerElement.getValue()) * 1000;
				}
			}
			return DEFAULT_SECONDS;
		};
	}
}
