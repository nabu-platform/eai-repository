package be.nabu.eai.repository.api;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public interface MCPTraceContext {
	public String getTraceId();
	public String getRootServiceId();
	public Writer getWriter();
	public Map<String, Object> getAttributes();
	public boolean isIncludeLinks();
	public int getDepth();
	public void write(String value) throws IOException;
	public void writeLine(String value) throws IOException;
	public void flush() throws IOException;
}
