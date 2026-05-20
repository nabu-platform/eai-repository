package be.nabu.eai.repository.api;

import java.io.IOException;

import be.nabu.libs.services.api.Service;

public interface MCPTraceProvider {
	public default void start(MCPTraceContext context, Service service) throws IOException {
		// do nothing
	}

	public default void stop(MCPTraceContext context, Service service) throws IOException {
		// do nothing
	}

	public default void error(MCPTraceContext context, Service service, Exception exception) throws IOException {
		// do nothing
	}

	public default void before(MCPTraceContext context, Object step) throws IOException {
		// do nothing
	}

	public default void after(MCPTraceContext context, Object step) throws IOException {
		// do nothing
	}

	public default void error(MCPTraceContext context, Object step, Exception exception) throws IOException {
		// do nothing
	}

	public default void describe(MCPTraceContext context, Object object) throws IOException {
		// do nothing
	}

	public default void report(MCPTraceContext context, Object object) throws IOException {
		// do nothing
	}
}
