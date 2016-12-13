package be.nabu.eai.repository.logger;

import javax.jws.WebParam;

public interface ServiceLogger {
	public void log(@WebParam(name = "log") NabuLogMessage message);
}
