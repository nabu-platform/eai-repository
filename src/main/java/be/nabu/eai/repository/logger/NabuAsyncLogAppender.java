package be.nabu.eai.repository.logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.services.api.DefinedService;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AsyncAppenderBase;

public class NabuAsyncLogAppender extends AsyncAppenderBase<ILoggingEvent> {

	private DefinedService service;
	private Repository repository;
	private List<String> stackOptionList = Arrays.asList("full");
	private Map<String, String> properties;
	
	public NabuAsyncLogAppender(Repository repository, DefinedService service, Map<String, String> properties) {
		this.repository = repository;
		this.service = service;
		this.properties = properties;
	}
	
	@Override
	protected void append(ILoggingEvent event) {
		NabuLogAppender.log(event, repository, service, stackOptionList, properties);
	}

}
